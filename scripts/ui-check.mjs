// Clicks through the chat page as two people (two tabs) in a headless Chrome and checks every flow:
// sign up, team, channel, add a person, chat both ways live, edit, delete, unread dot, log out.
//
// 1. Start the backend:        ./mvnw spring-boot:run
// 2. Start a headless Chrome:  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
//                                --headless=new --remote-debugging-port=9333 --user-data-dir=/tmp/minichat-headless about:blank
// 3. Run:                      node scripts/ui-check.mjs [folder-for-screenshots]
//
// To let REA record the run (see README, "Check the rebuild with REA"), open the page in a tab first,
// start REA on that tab, then run with ALICE_TARGET=<tab id> START_DELAY_MS=25000.
// Needs Node 22 or newer (built-in fetch and WebSocket). No npm packages.
import { writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';

const CDP = 'http://127.0.0.1:9333';
const APP = 'http://localhost:8090/';
const OUT = process.argv[2] || tmpdir();
const stamp = Date.now().toString(36);
const alice = { username: 'alice' + stamp, email: 'alice' + stamp + '@example.com' };
const bob = { username: 'bob' + stamp, email: 'bob' + stamp + '@example.com' };
const PASSWORD = 'masterclass-demo-1';

const results = [];
function check(name, ok, detail = '') {
	results.push({ name, ok });
	console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok || !detail ? '' : '  -> ' + detail));
}

async function openTab(existingTargetId) {
	const target = existingTargetId
		? (await (await fetch(CDP + '/json/list')).json()).find((t) => t.id === existingTargetId)
		: await (await fetch(CDP + '/json/new?about:blank', { method: 'PUT' })).json();
	const socket = new WebSocket(target.webSocketDebuggerUrl);
	await new Promise((resolve, reject) => {
		socket.addEventListener('open', resolve);
		socket.addEventListener('error', reject);
	});
	let nextId = 1;
	const pending = new Map();
	const problems = [];
	socket.addEventListener('message', (event) => {
		const message = JSON.parse(event.data);
		if (message.id && pending.has(message.id)) {
			pending.get(message.id)(message);
			pending.delete(message.id);
		} else if (message.method === 'Runtime.exceptionThrown') {
			problems.push('exception: ' + (message.params.exceptionDetails.exception?.description || message.params.exceptionDetails.text));
		} else if (message.method === 'Runtime.consoleAPICalled' && message.params.type === 'error') {
			problems.push('console.error: ' + message.params.args.map((a) => a.value ?? a.description).join(' '));
		}
	});
	const send = (method, params = {}) => new Promise((resolve) => {
		const id = nextId++;
		pending.set(id, resolve);
		socket.send(JSON.stringify({ id, method, params }));
	});
	const run = async (expression) => {
		const reply = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
		if (reply.result.exceptionDetails) {
			throw new Error(reply.result.exceptionDetails.exception?.description || reply.result.exceptionDetails.text);
		}
		return reply.result.result.value;
	};
	const until = async (expression, label, timeoutMs = 6000) => {
		const started = Date.now();
		while (Date.now() - started < timeoutMs) {
			if (await run(expression)) return true;
			await new Promise((resolve) => setTimeout(resolve, 100));
		}
		return false;
	};
	await send('Runtime.enable');
	await send('Page.enable');
	return { send, run, until, problems };
}

const visible = (id) => `!document.getElementById('${id}').hidden`;
const texts = `[...document.querySelectorAll('#message-list .message-text')].map(e => e.textContent)`;
const fill = (id, value) => `document.getElementById('${id}').value = ${JSON.stringify(value)}`;

async function signUp(tab, person) {
	// A tab that REA is already recording is left on the page it has, so the recording is not interrupted.
	if (!tab.preloaded) await tab.send('Page.navigate', { url: APP });
	await tab.until(visible('auth-screen'), 'auth screen');
	await tab.run(`document.getElementById('tab-signup').click()`);
	await tab.run(fill('signup-email', person.email));
	await tab.run(fill('signup-username', person.username));
	await tab.run(fill('signup-password', PASSWORD));
	await tab.run(`document.getElementById('signup-form').requestSubmit()`);
	return tab.until(visible('chat-screen'), 'chat screen');
}

async function dialog(tab, buttonId, value) {
	await tab.run(`document.getElementById('${buttonId}').click()`);
	await tab.until(`document.getElementById('dialog').open`, 'dialog open');
	await tab.run(fill('dialog-input', value));
	await tab.run(`document.getElementById('dialog-form').requestSubmit()`);
	return tab.until(`!document.getElementById('dialog').open`, 'dialog closed');
}

async function sendMessage(tab, text) {
	await tab.run(fill('composer-input', text));
	await tab.run(`document.getElementById('composer').requestSubmit()`);
}

// Optional pause so a recorder (REA) can finish attaching to the tab before anything is clicked.
await new Promise((resolve) => setTimeout(resolve, Number(process.env.START_DELAY_MS || 0)));
const a = await openTab(process.env.ALICE_TARGET);
a.preloaded = Boolean(process.env.ALICE_TARGET);
const b = await openTab();

// --- Alice signs up, sees the "no team" hint, creates a team and a channel
check('alice: sign up lands on the chat screen', await signUp(a, alice));
check('alice: no-team hint is shown', await a.until(`${visible('empty-state')} && document.getElementById('empty-state').textContent.includes('not in a team')`));
check('alice: create team closes the dialog', await dialog(a, 'new-team-button', 'Masterclass ' + stamp));
check('alice: team appears in the selector', await a.until(`document.getElementById('team-select').options.length === 1`));
check('alice: no-channel hint is shown', await a.until(`document.getElementById('empty-state').textContent.includes('no channels yet')`));
check('alice: create channel closes the dialog', await dialog(a, 'new-channel-button', 'General'));
check('alice: channel opens', await a.until(`document.getElementById('channel-title').textContent === '# General' && ${visible('composer')}`));
check('alice: live status turns on', await a.until(`document.getElementById('live-status').textContent === 'Live'`));
check('alice: empty channel says so', await a.until(`document.getElementById('empty-state').textContent.includes('No messages yet')`));

// --- Duplicate team name shows the server's message inside the dialog and keeps it open
await a.run(`document.getElementById('new-team-button').click()`);
await a.run(fill('dialog-input', 'Masterclass ' + stamp));
await a.run(`document.getElementById('dialog-form').requestSubmit()`);
check('alice: duplicate team name is explained in the dialog', await a.until(`document.getElementById('dialog').open && document.getElementById('dialog-error').textContent.includes('already exists')`));
await a.run(`document.getElementById('dialog-cancel').click()`);

// --- Bob signs up; Alice adds him; unknown email is explained
check('bob: sign up lands on the chat screen', await signUp(b, bob));
await a.run(`document.getElementById('invite-button').click()`);
await a.run(fill('dialog-input', 'nobody' + stamp + '@example.com'));
await a.run(`document.getElementById('dialog-form').requestSubmit()`);
check('alice: adding an unknown email is explained', await a.until(`document.getElementById('dialog-error').textContent.includes('No account uses this email')`));
await a.run(`document.getElementById('dialog-cancel').click()`);
check('alice: add bob closes the dialog', await dialog(a, 'invite-button', bob.email));
check('alice: bob is listed under People', await a.until(`document.getElementById('member-list').textContent.includes('@${bob.username}')`));

// --- Bob reloads (token kept in sessionStorage) and lands in the team's channel
await b.send('Page.reload');
check('bob: after reload he is still logged in and sees the channel', await b.until(`${visible('chat-screen')} && document.getElementById('channel-title').textContent === '# General'`));
check('bob: live status turns on', await b.until(`document.getElementById('live-status').textContent === 'Live'`));

// --- Messages flow both ways live
await sendMessage(a, 'hello from alice');
check('alice: her message appears', await a.until(`${texts}.includes('hello from alice')`));
check('bob: receives it live without reloading', await b.until(`${texts}.includes('hello from alice')`));
check('alice: composer is cleared after sending', await a.run(`document.getElementById('composer-input').value === ''`));
await sendMessage(b, 'hi alice');
check('alice: receives bob\'s reply live', await a.until(`${texts}.includes('hi alice')`));
check('alice: message is not duplicated (REST reply + live event)', await a.run(`${texts}.filter(t => t === 'hello from alice').length === 1`));
check('bob: sees the author name', await b.until(`[...document.querySelectorAll('#message-list .author')].some(e => e.textContent === ${JSON.stringify(alice.username)})`));

// --- Text that looks like HTML stays text
const evil = '<img src=x onerror="window.__xss = 1"> <b>bold?</b>';
await sendMessage(a, evil);
check('bob: HTML-looking text is shown as plain text', await b.until(`${texts}.includes(${JSON.stringify(evil)})`));
check('bob: no element was created from the message', await b.run(`document.querySelectorAll('#message-list img, #message-list b').length === 0 && window.__xss === undefined`));

// --- Edit and delete: only on own messages, and the other person sees the change live
check('bob: no Edit button on alice\'s messages', await b.run(`[...document.querySelectorAll('#message-list .message')].filter(m => m.querySelector('.author').textContent === ${JSON.stringify(alice.username)}).every(m => !m.querySelector('.message-actions'))`));
await a.run(`[...document.querySelectorAll('#message-list .message')].find(m => m.querySelector('.message-text').textContent === 'hello from alice').querySelector('.message-actions button').click()`);
await a.until(`!!document.querySelector('.edit-form input')`, 'edit form');
await a.run(`document.querySelector('.edit-form input').value = 'hello from alice (fixed)'`);
await a.run(`document.querySelector('.edit-form').requestSubmit()`);
check('alice: edited text replaces the old one', await a.until(`${texts}.includes('hello from alice (fixed)') && !${texts}.includes('hello from alice')`));
check('bob: sees the edit live, marked as edited', await b.until(`${texts}.includes('hello from alice (fixed)') && document.querySelectorAll('#message-list .edited').length === 1`));

await a.run(`window.confirm = () => true`);
await a.run(`[...document.querySelectorAll('#message-list .message')].find(m => m.querySelector('.message-text').textContent.startsWith('<img')).querySelector('.message-actions .danger').click()`);
check('alice: deleted message disappears', await a.until(`!${texts}.some(t => t.startsWith('<img'))`));
check('bob: deletion arrives live', await b.until(`!${texts}.some(t => t.startsWith('<img'))`));

// --- A second channel: bob learns about it and gets an unread dot
check('alice: create second channel', await dialog(a, 'new-channel-button', 'Random Talk'));
await a.until(`document.getElementById('channel-title').textContent === '# Random Talk'`, 'second channel');
await sendMessage(a, 'anyone here?');
check('bob: new channel shows up with an unread dot', await b.until(`[...document.querySelectorAll('#channel-list button')].some(btn => btn.textContent.includes('Random Talk') && btn.querySelector('.dot'))`));
check('bob: stays in the channel he was reading', await b.run(`document.getElementById('channel-title').textContent === '# General'`));
await b.run(`[...document.querySelectorAll('#channel-list button')].find(btn => btn.textContent.includes('Random Talk')).click()`);
check('bob: opening it shows the message and clears the dot', await b.until(`${texts}.includes('anyone here?') && !document.querySelector('#channel-list .dot')`));

// --- Screenshot of alice's window, then bob logs out
await a.run(`[...document.querySelectorAll('#channel-list button')].find(btn => btn.textContent.includes('General')).click()`);
await a.until(`${texts}.includes('hi alice')`, 'back to general');
const shot = await a.send('Page.captureScreenshot', { format: 'png' });
writeFileSync(OUT + '/alice.png', Buffer.from(shot.result.data, 'base64'));

await b.run(`document.getElementById('logout-button').click()`);
check('bob: logout returns to the login screen', await b.until(`${visible('auth-screen')} && sessionStorage.getItem('minichat.token') === null`));
await b.run(fill('login-id', bob.username));
await b.run(fill('login-password', 'wrong-password-123'));
await b.run(`document.getElementById('login-form').requestSubmit()`);
check('bob: wrong password is explained and he stays on the login screen', await b.until(`document.getElementById('auth-error').textContent.includes('valid email or username and password') && ${visible('auth-screen')}`));
await b.run(fill('login-password', PASSWORD));
await b.run(`document.getElementById('login-form').requestSubmit()`);
check('bob: correct password logs him back in', await b.until(`${visible('chat-screen')}`));
const loginShot = await b.send('Page.captureScreenshot', { format: 'png' });
writeFileSync(OUT + '/bob.png', Buffer.from(loginShot.result.data, 'base64'));

check('no JavaScript errors in alice\'s tab', a.problems.length === 0, a.problems.join(' | '));
// bob's wrong-password attempt makes the browser log the 401 itself; that one line is expected
const bobProblems = b.problems.filter((p) => !p.includes('401'));
check('no JavaScript errors in bob\'s tab', bobProblems.length === 0, bobProblems.join(' | '));

const failed = results.filter((r) => !r.ok);
console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
process.exit(failed.length ? 1 : 0);
