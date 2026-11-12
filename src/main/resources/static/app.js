'use strict';

/*
 * Mini Chat web page. Plain JavaScript, no build step.
 * It talks to the same 12 REST calls and the WebSocket that were rebuilt from the REA recording.
 * All user-written text is put on the page with textContent, never as HTML.
 */

const TOKEN_KEY = 'minichat.token';
const TEAM_KEY = 'minichat.team';

const state = {
	token: sessionStorage.getItem(TOKEN_KEY),
	me: null,
	teams: [],
	teamId: null,
	channels: [],
	channelId: null,
	members: new Map(), // user id -> username
	posts: new Map(), // post id -> post, for the open channel
	unread: new Set(), // channel ids with new messages
	socket: null,
	retryMs: 1000,
	wasLive: false,
};

const $ = (id) => document.getElementById(id);

/* ---------- talking to the backend ---------- */

async function api(method, path, body) {
	const headers = {};
	if (state.token) headers.Authorization = 'Bearer ' + state.token;
	if (body !== undefined) headers['Content-Type'] = 'application/json';

	let response;
	try {
		response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
	} catch (networkError) {
		throw new Error('Cannot reach the server. Check that it is running, then try again.');
	}

	const text = await response.text();
	let data = null;
	try {
		data = text ? JSON.parse(text) : null;
	} catch (parseError) {
		data = null;
	}

	if (!response.ok) {
		if (response.status === 401 && state.token) {
			logout('Your session ended. Please log in again.');
		}
		const error = new Error(errorText(data, response.status));
		error.status = response.status;
		throw error;
	}
	return { data, token: response.headers.get('Token') };
}

function errorText(data, status) {
	if (data && data.message) {
		return data.detailed_error ? data.message + ' ' + data.detailed_error : data.message;
	}
	return 'Something went wrong (status ' + status + '). Please try again.';
}

/* ---------- sign up / log in / log out ---------- */

function showAuth(message) {
	$('chat-screen').hidden = true;
	$('auth-screen').hidden = false;
	showError('auth-error', message || '');
}

function switchTab(signup) {
	$('login-form').hidden = signup;
	$('signup-form').hidden = !signup;
	$('tab-login').classList.toggle('active', !signup);
	$('tab-signup').classList.toggle('active', signup);
	$('tab-login').setAttribute('aria-selected', String(!signup));
	$('tab-signup').setAttribute('aria-selected', String(signup));
	showError('auth-error', '');
}

async function login(loginId, password) {
	const result = await api('POST', '/api/v4/users/login', { login_id: loginId, password });
	state.token = result.token;
	sessionStorage.setItem(TOKEN_KEY, state.token);
	await start();
}

async function onLoginSubmit(event) {
	event.preventDefault();
	await withButton(event.submitter, async () => {
		try {
			await login($('login-id').value.trim(), $('login-password').value);
			$('login-password').value = '';
		} catch (error) {
			showError('auth-error', error.message);
		}
	});
}

async function onSignupSubmit(event) {
	event.preventDefault();
	await withButton(event.submitter, async () => {
		const email = $('signup-email').value.trim();
		const username = $('signup-username').value.trim();
		const password = $('signup-password').value;
		try {
			await api('POST', '/api/v4/users', { email, username, password });
			await login(username, password);
			$('signup-password').value = '';
		} catch (error) {
			showError('auth-error', error.message);
		}
	});
}

async function logout(message) {
	const token = state.token;
	state.token = null;
	sessionStorage.removeItem(TOKEN_KEY);
	if (state.socket) {
		const socket = state.socket;
		state.socket = null;
		socket.close();
	}
	state.me = null;
	state.teams = [];
	state.teamId = null;
	state.channels = [];
	state.channelId = null;
	state.posts.clear();
	state.unread.clear();
	showAuth(message);
	if (token && !message) {
		// Tell the server to forget the token. If this fails the page is already logged out locally.
		try {
			await fetch('/api/v4/users/logout', { method: 'POST', headers: { Authorization: 'Bearer ' + token } });
		} catch (ignored) {
			// nothing more to do
		}
	}
}

/* ---------- starting the chat screen ---------- */

async function start() {
	try {
		state.me = (await api('GET', '/api/v4/users/me')).data;
	} catch (error) {
		if (state.token) showAuth(error.message);
		return;
	}
	$('auth-screen').hidden = true;
	$('chat-screen').hidden = false;
	$('me-name').textContent = '@' + state.me.username;
	showError('chat-error', '');
	connectLive();
	await loadTeams();
}

async function loadTeams(preferTeamId) {
	try {
		state.teams = (await api('GET', '/api/v4/users/me/teams')).data;
	} catch (error) {
		showError('chat-error', error.message);
		return;
	}
	const select = $('team-select');
	select.replaceChildren();
	for (const team of state.teams) {
		const option = document.createElement('option');
		option.value = team.id;
		option.textContent = team.display_name;
		select.append(option);
	}
	select.disabled = state.teams.length === 0;

	if (state.teams.length === 0) {
		state.teamId = null;
		state.channels = [];
		state.channelId = null;
		renderChannels();
		renderMembers();
		showEmpty('You are not in a team yet. Create one with "+ New team", or ask a teammate to add your email.');
		return;
	}
	const wanted = preferTeamId || sessionStorage.getItem(TEAM_KEY);
	const team = state.teams.find((item) => item.id === wanted) || state.teams[0];
	select.value = team.id;
	await selectTeam(team.id);
}

async function selectTeam(teamId, preferChannelId) {
	state.teamId = teamId;
	sessionStorage.setItem(TEAM_KEY, teamId);
	state.unread.clear();
	try {
		const [channels, members] = await Promise.all([
			api('GET', '/api/v4/users/me/teams/' + teamId + '/channels'),
			api('GET', '/api/v4/users?in_team=' + teamId + '&per_page=200'),
		]);
		if (state.teamId !== teamId) return; // the person switched team while this was loading
		state.channels = channels.data;
		state.members = new Map(members.data.map((user) => [user.id, user.username]));
	} catch (error) {
		showError('chat-error', error.message);
		return;
	}
	renderMembers();
	const channel = state.channels.find((item) => item.id === preferChannelId) || state.channels[0];
	if (!channel) {
		state.channelId = null;
		renderChannels();
		showEmpty('This team has no channels yet. Create one with the + next to Channels.');
		return;
	}
	await openChannel(channel.id);
}

async function openChannel(channelId) {
	state.channelId = channelId;
	state.unread.delete(channelId);
	renderChannels();
	const channel = state.channels.find((item) => item.id === channelId);
	$('channel-title').textContent = channel ? '# ' + channel.display_name : 'Channel';
	let listing;
	try {
		listing = (await api('GET', '/api/v4/channels/' + channelId + '/posts?per_page=200')).data;
	} catch (error) {
		showError('chat-error', error.message);
		return;
	}
	if (state.channelId !== channelId) return; // another channel was opened meanwhile
	state.posts = new Map(listing.order.map((id) => [id, listing.posts[id]]));
	showError('chat-error', '');
	renderMessages();
	$('composer').hidden = false;
	$('composer-input').focus();
}

/* ---------- drawing ---------- */

function showError(elementId, message) {
	const element = $(elementId);
	element.textContent = message;
	element.hidden = !message;
}

function showEmpty(message) {
	$('channel-title').textContent = 'Mini Chat';
	$('message-list').replaceChildren();
	$('message-list').hidden = true;
	$('composer').hidden = true;
	$('empty-state').textContent = message;
	$('empty-state').hidden = false;
}

function renderChannels() {
	const list = $('channel-list');
	list.replaceChildren();
	for (const channel of state.channels) {
		const item = document.createElement('li');
		const button = document.createElement('button');
		button.type = 'button';
		const label = document.createElement('span');
		label.textContent = '# ' + channel.display_name;
		button.append(label);
		if (channel.id === state.channelId) button.setAttribute('aria-current', 'true');
		if (state.unread.has(channel.id)) {
			label.className = 'unread';
			const dot = document.createElement('span');
			dot.className = 'dot';
			dot.title = 'New messages';
			dot.setAttribute('aria-label', 'new messages');
			button.append(dot);
		}
		button.addEventListener('click', () => openChannel(channel.id));
		item.append(button);
		list.append(item);
	}
}

function renderMembers() {
	const list = $('member-list');
	list.replaceChildren();
	const names = [...state.members.values()].sort();
	for (const name of names) {
		const item = document.createElement('li');
		item.textContent = '@' + name;
		list.append(item);
	}
}

function sortedPosts() {
	return [...state.posts.values()].sort((a, b) => a.create_at - b.create_at || a.id.localeCompare(b.id));
}

function renderMessages() {
	const list = $('message-list');
	list.replaceChildren();
	$('empty-state').hidden = true;
	list.hidden = false;
	const posts = sortedPosts();
	if (posts.length === 0) {
		$('empty-state').textContent = 'No messages yet. Say hello.';
		$('empty-state').hidden = false;
		list.hidden = true;
		return;
	}
	for (const post of posts) list.append(messageElement(post));
	list.scrollTop = list.scrollHeight;
}

function messageElement(post) {
	const author = state.members.get(post.user_id) || 'someone';
	const item = document.createElement('li');
	item.className = 'message';
	item.dataset.postId = post.id;

	const avatar = document.createElement('div');
	avatar.className = 'avatar';
	avatar.setAttribute('aria-hidden', 'true');
	avatar.textContent = author.charAt(0);

	const body = document.createElement('div');
	const head = document.createElement('div');
	head.className = 'message-head';

	const name = document.createElement('span');
	name.className = 'author';
	name.textContent = author;
	const time = document.createElement('time');
	time.className = 'time';
	const when = new Date(post.create_at);
	time.dateTime = when.toISOString();
	time.textContent = when.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
	head.append(name, time);

	if (post.edit_at > 0) {
		const edited = document.createElement('span');
		edited.className = 'edited';
		edited.textContent = '(edited)';
		head.append(edited);
	}

	if (state.me && post.user_id === state.me.id) {
		const actions = document.createElement('span');
		actions.className = 'message-actions';
		const edit = document.createElement('button');
		edit.type = 'button';
		edit.textContent = 'Edit';
		edit.addEventListener('click', () => startEdit(post.id));
		const remove = document.createElement('button');
		remove.type = 'button';
		remove.className = 'danger';
		remove.textContent = 'Delete';
		remove.addEventListener('click', () => deletePost(post.id));
		actions.append(edit, remove);
		head.append(actions);
	}

	const text = document.createElement('p');
	text.className = 'message-text';
	text.textContent = post.message;

	body.append(head, text);
	item.append(avatar, body);
	return item;
}

function findMessageElement(postId) {
	for (const item of $('message-list').children) {
		if (item.dataset.postId === postId) return item;
	}
	return null;
}

/** Adds or replaces one post on screen without redrawing the whole list. */
function upsertPost(post) {
	const list = $('message-list');
	const nearBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 80;
	const isNew = !state.posts.has(post.id);
	state.posts.set(post.id, post);
	const existing = findMessageElement(post.id);
	if (existing) {
		existing.replaceWith(messageElement(post));
	} else if (state.posts.size === 1) {
		renderMessages();
	} else {
		list.append(messageElement(post));
	}
	const mine = state.me && post.user_id === state.me.id;
	if (isNew && (nearBottom || mine)) list.scrollTop = list.scrollHeight;
}

function removePost(postId) {
	if (!state.posts.delete(postId)) return;
	const existing = findMessageElement(postId);
	if (existing) existing.remove();
	if (state.posts.size === 0) renderMessages();
}

/* ---------- sending, editing, deleting ---------- */

async function onComposerSubmit(event) {
	event.preventDefault();
	const input = $('composer-input');
	const message = input.value;
	if (!message.trim() || !state.channelId) return;
	const button = $('composer').querySelector('button');
	await withButton(button, async () => {
		try {
			const post = (await api('POST', '/api/v4/posts', { channel_id: state.channelId, message })).data;
			input.value = '';
			resizeComposer();
			showError('chat-error', '');
			if (post.channel_id === state.channelId) upsertPost(post);
		} catch (error) {
			// The text stays in the box so nothing the person wrote is lost.
			showError('chat-error', 'Your message was not sent. ' + error.message);
		}
	});
	input.focus();
}

function startEdit(postId) {
	const post = state.posts.get(postId);
	const item = findMessageElement(postId);
	if (!post || !item || item.querySelector('.edit-form')) return;
	const text = item.querySelector('.message-text');

	const form = document.createElement('form');
	form.className = 'edit-form';
	const input = document.createElement('input');
	input.value = post.message;
	input.maxLength = 16000;
	input.setAttribute('aria-label', 'Edit message');
	const save = document.createElement('button');
	save.type = 'submit';
	save.className = 'primary';
	save.textContent = 'Save';
	const cancel = document.createElement('button');
	cancel.type = 'button';
	cancel.textContent = 'Cancel';
	form.append(input, save, cancel);

	const stop = () => form.replaceWith(text);
	cancel.addEventListener('click', stop);
	input.addEventListener('keydown', (event) => {
		if (event.key === 'Escape') stop();
	});
	form.addEventListener('submit', async (event) => {
		event.preventDefault();
		const message = input.value;
		if (!message.trim() || message === post.message) {
			stop();
			return;
		}
		await withButton(save, async () => {
			try {
				const updated = (await api('PUT', '/api/v4/posts/' + postId + '/patch', { message })).data;
				showError('chat-error', '');
				if (updated.channel_id === state.channelId) upsertPost(updated);
			} catch (error) {
				showError('chat-error', 'Your edit was not saved. ' + error.message);
			}
		});
	});

	text.replaceWith(form);
	input.focus();
}

async function deletePost(postId) {
	if (!window.confirm('Delete this message? This cannot be undone.')) return;
	try {
		await api('DELETE', '/api/v4/posts/' + postId);
		showError('chat-error', '');
		removePost(postId);
	} catch (error) {
		if (error.status === 404) {
			removePost(postId); // someone already deleted it
			return;
		}
		showError('chat-error', 'The message was not deleted. ' + error.message);
	}
}

/* ---------- live updates ---------- */

function setLive(on) {
	const status = $('live-status');
	status.textContent = on ? 'Live' : 'Reconnecting…';
	status.classList.toggle('on', on);
}

function connectLive() {
	if (!state.token) return;
	const scheme = location.protocol === 'https:' ? 'wss://' : 'ws://';
	const socket = new WebSocket(scheme + location.host + '/api/v4/websocket');
	state.socket = socket;

	socket.addEventListener('open', () => {
		socket.send(JSON.stringify({ action: 'authentication_challenge', seq: 1, data: { token: state.token } }));
	});

	socket.addEventListener('message', (event) => {
		let frame;
		try {
			frame = JSON.parse(event.data);
		} catch (parseError) {
			return;
		}
		if (frame.seq_reply === 1 && frame.status === 'OK') {
			setLive(true);
			state.retryMs = 1000;
			// After a dropped connection, reload the open channel to pick up anything that was missed.
			if (state.wasLive && state.channelId) openChannel(state.channelId);
			state.wasLive = true;
		} else if (frame.event) {
			handleLiveEvent(frame);
		}
	});

	socket.addEventListener('close', () => {
		if (state.socket !== socket) return; // logged out, or replaced by a newer connection
		setLive(false);
		const wait = state.retryMs;
		state.retryMs = Math.min(state.retryMs * 2, 15000);
		setTimeout(() => {
			if (state.socket === socket) connectLive();
		}, wait);
	});
}

function handleLiveEvent(frame) {
	let post;
	try {
		post = JSON.parse(frame.data.post);
	} catch (parseError) {
		return;
	}
	const teamId = frame.broadcast && frame.broadcast.team_id;
	if (teamId !== state.teamId) return;

	if (frame.event === 'posted') {
		if (!state.members.has(post.user_id) && frame.data.sender_name) {
			state.members.set(post.user_id, String(frame.data.sender_name).replace(/^@/, ''));
			renderMembers();
		}
		if (!state.channels.some((channel) => channel.id === post.channel_id)) {
			reloadChannels(post.channel_id); // a teammate created a channel we have not listed yet
		} else if (post.channel_id === state.channelId) {
			upsertPost(post);
		} else {
			state.unread.add(post.channel_id);
			renderChannels();
		}
	} else if (frame.event === 'post_edited') {
		if (post.channel_id === state.channelId && state.posts.has(post.id)) upsertPost(post);
	} else if (frame.event === 'post_deleted') {
		if (post.channel_id === state.channelId) removePost(post.id);
	}
}

async function reloadChannels(unreadChannelId) {
	const teamId = state.teamId;
	try {
		const channels = (await api('GET', '/api/v4/users/me/teams/' + teamId + '/channels')).data;
		if (state.teamId !== teamId) return;
		state.channels = channels;
		if (unreadChannelId && unreadChannelId !== state.channelId) state.unread.add(unreadChannelId);
		renderChannels();
		if (!state.channelId && channels.length > 0) await openChannel(channels[0].id);
	} catch (error) {
		showError('chat-error', error.message);
	}
}

/* ---------- small dialog: new team, new channel, add a person ---------- */

let dialogSubmit = null;

function openDialog(options) {
	$('dialog-title').textContent = options.title;
	$('dialog-help').textContent = options.help;
	$('dialog-label').textContent = options.label;
	$('dialog-submit').textContent = options.submit;
	const input = $('dialog-input');
	input.value = '';
	input.type = options.type || 'text';
	input.maxLength = options.maxLength || 64;
	showError('dialog-error', '');
	dialogSubmit = options.onSubmit;
	$('dialog').showModal();
	input.focus();
}

async function onDialogSubmit(event) {
	event.preventDefault();
	const value = $('dialog-input').value.trim();
	if (!value || !dialogSubmit) return;
	await withButton($('dialog-submit'), async () => {
		try {
			await dialogSubmit(value);
			$('dialog').close();
		} catch (error) {
			showError('dialog-error', error.message);
		}
	});
}

/** "Design Team" -> "design-team": the short name the backend wants. */
function slug(displayName) {
	return displayName.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}

function newTeam() {
	openDialog({
		title: 'New team',
		help: 'A team is a group of people who share channels.',
		label: 'Team name',
		submit: 'Create team',
		onSubmit: async (displayName) => {
			const name = slug(displayName);
			if (name.length < 2) throw new Error('Use at least two letters or digits in the name.');
			const team = (await api('POST', '/api/v4/teams', { name, display_name: displayName, type: 'O' })).data;
			await loadTeams(team.id);
		},
	});
}

function newChannel() {
	if (!state.teamId) {
		showError('chat-error', 'Create a team first, then add channels to it.');
		return;
	}
	openDialog({
		title: 'New channel',
		help: 'Everyone in the team can read and write in it.',
		label: 'Channel name',
		submit: 'Create channel',
		onSubmit: async (displayName) => {
			const name = slug(displayName);
			if (name.length < 1) throw new Error('Use at least one letter or digit in the name.');
			const body = { team_id: state.teamId, name, display_name: displayName, type: 'O' };
			const channel = (await api('POST', '/api/v4/channels', body)).data;
			await selectTeam(state.teamId, channel.id);
		},
	});
}

function addPerson() {
	if (!state.teamId) {
		showError('chat-error', 'Create a team first, then add people to it.');
		return;
	}
	openDialog({
		title: 'Add a person',
		help: 'They need an account first. Enter the email they signed up with.',
		label: 'Email',
		submit: 'Add to team',
		type: 'email',
		maxLength: 128,
		onSubmit: async (email) => {
			const results = (await api('POST', '/api/v4/teams/' + state.teamId + '/invite/email', [email])).data;
			if (results[0] && results[0].error) throw new Error(results[0].error.message);
			const members = (await api('GET', '/api/v4/users?in_team=' + state.teamId + '&per_page=200')).data;
			state.members = new Map(members.map((user) => [user.id, user.username]));
			renderMembers();
		},
	});
}

/* ---------- helpers ---------- */

/** Disables a button while its action runs, so a double click cannot send things twice. */
async function withButton(button, action) {
	if (button) button.disabled = true;
	try {
		await action();
	} finally {
		if (button) button.disabled = false;
	}
}

function resizeComposer() {
	const input = $('composer-input');
	input.style.height = 'auto';
	input.style.height = Math.min(input.scrollHeight, 140) + 'px';
}

/* ---------- wiring ---------- */

$('tab-login').addEventListener('click', () => switchTab(false));
$('tab-signup').addEventListener('click', () => switchTab(true));
$('login-form').addEventListener('submit', onLoginSubmit);
$('signup-form').addEventListener('submit', onSignupSubmit);
$('logout-button').addEventListener('click', () => logout());
$('team-select').addEventListener('change', (event) => selectTeam(event.target.value));
$('new-team-button').addEventListener('click', newTeam);
$('new-channel-button').addEventListener('click', newChannel);
$('invite-button').addEventListener('click', addPerson);
$('dialog-form').addEventListener('submit', onDialogSubmit);
$('dialog-cancel').addEventListener('click', () => $('dialog').close());
$('composer').addEventListener('submit', onComposerSubmit);
$('composer-input').addEventListener('input', resizeComposer);
$('composer-input').addEventListener('keydown', (event) => {
	if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
		event.preventDefault();
		$('composer').requestSubmit();
	}
});
// Someone may have added this person to a team while the tab was in the background.
document.addEventListener('visibilitychange', () => {
	if (!document.hidden && state.token && state.me && state.teams.length === 0) loadTeams();
});

if (state.token) {
	start();
} else {
	showAuth();
}
