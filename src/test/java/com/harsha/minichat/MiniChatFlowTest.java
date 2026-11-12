package com.harsha.minichat;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Starts the real application on a random port with a real Postgres (Testcontainers) and replays the flow
 * that REA recorded: sign up, log in, create team, create channel, send, read, edit and delete messages.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MiniChatFlowTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String PASSWORD = "correct-horse-battery";

	/** Field names REA recorded in the response of POST /api/v4/posts. */
	private static final Set<String> RECORDED_POST_FIELDS = Set.of(
			"channel_id", "create_at", "delete_at", "edit_at", "file_ids", "hashtags", "id", "is_pinned",
			"last_reply_at", "message", "metadata", "original_id", "participants", "pending_post_id", "props",
			"remote_id", "reply_count", "root_id", "type", "update_at", "user_id");

	private final HttpClient http = HttpClient.newHttpClient();

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void recordedChatFlowWorksEndToEnd() throws Exception {
		// Sign up
		Reply signUp = call("POST", "/api/v4/users", null,
				"{\"email\":\"asha@example.com\",\"username\":\"asha\",\"password\":\"" + PASSWORD + "\"}");
		assertThat(signUp.status).isEqualTo(201);
		String userId = signUp.body.get("id").asString();
		assertThat(userId).matches("[a-z0-9]{26}");
		assertThat(signUp.body.get("username").asString()).isEqualTo("asha");
		assertThat(signUp.raw).doesNotContain("password");

		// Log in the way the recording did (by user id) and the way a person does (by email)
		Reply loginById = call("POST", "/api/v4/users/login", null,
				"{\"id\":\"" + userId + "\",\"password\":\"" + PASSWORD + "\",\"token\":\"\",\"device_id\":\"\"}");
		assertThat(loginById.status).isEqualTo(200);
		assertThat(loginById.token).matches("[a-z0-9]{26}");
		Reply loginByEmail = call("POST", "/api/v4/users/login", null,
				"{\"login_id\":\"asha@example.com\",\"password\":\"" + PASSWORD + "\"}");
		assertThat(loginByEmail.status).isEqualTo(200);
		String token = loginByEmail.token;

		Reply me = call("GET", "/api/v4/users/me", token, null);
		assertThat(me.status).isEqualTo(200);
		assertThat(me.body.get("email").asString()).isEqualTo("asha@example.com");

		// Team
		Reply team = call("POST", "/api/v4/teams", token,
				"{\"name\":\"masterclass\",\"display_name\":\"Masterclass\",\"type\":\"O\",\"id\":\"ignored\",\"create_at\":5}");
		assertThat(team.status).isEqualTo(201);
		String teamId = team.body.get("id").asString();
		assertThat(teamId).isNotEqualTo("ignored");
		Reply myTeams = call("GET", "/api/v4/users/me/teams", token, null);
		assertThat(myTeams.status).isEqualTo(200);
		assertThat(myTeams.body.isArray()).isTrue();
		assertThat(myTeams.body.get(0).get("name").asString()).isEqualTo("masterclass");

		// Channel
		Reply channel = call("POST", "/api/v4/channels", token,
				"{\"team_id\":\"" + teamId + "\",\"name\":\"demo\",\"display_name\":\"Demo\",\"type\":\"O\",\"creator_id\":\"someone-else\"}");
		assertThat(channel.status).isEqualTo(201);
		String channelId = channel.body.get("id").asString();
		assertThat(channel.body.get("creator_id").asString()).isEqualTo(userId);
		Reply channels = call("GET", "/api/v4/users/me/teams/" + teamId + "/channels", token, null);
		assertThat(channels.status).isEqualTo(200);
		assertThat(channels.body.size()).isEqualTo(1);

		// Send three messages
		List<String> postIds = new ArrayList<>();
		for (String text : List.of("first", "second", "third")) {
			Reply post = call("POST", "/api/v4/posts", token,
					"{\"channel_id\":\"" + channelId + "\",\"message\":\"" + text + "\",\"user_id\":\"someone-else\",\"create_at\":1}");
			assertThat(post.status).isEqualTo(201);
			assertThat(post.body.get("user_id").asString()).isEqualTo(userId);
			assertThat(post.body.get("create_at").asLong()).isGreaterThan(1);
			assertThat(fieldNames(post.body)).isEqualTo(new TreeSet<>(RECORDED_POST_FIELDS));
			postIds.add(post.body.get("id").asString());
			Thread.sleep(5); // keep create_at different so "newest first" is well defined
		}

		// Read: newest first, each id present in the posts map
		Reply list = call("GET", "/api/v4/channels/" + channelId + "/posts", token, null);
		assertThat(list.status).isEqualTo(200);
		assertThat(strings(list.body.get("order"))).containsExactly(postIds.get(2), postIds.get(1), postIds.get(0));
		assertThat(list.body.get("posts").get(postIds.get(0)).get("message").asString()).isEqualTo("first");
		assertThat(fieldNames(list.body)).containsExactlyInAnyOrder(
				"order", "posts", "next_post_id", "prev_post_id", "first_inaccessible_post_time");
		long beforeChanges = list.body.get("posts").get(postIds.get(2)).get("update_at").asLong();

		// Edit the second message; the client sends the whole post back, only message is taken
		Reply edit = call("PUT", "/api/v4/posts/" + postIds.get(1) + "/patch", token,
				"{\"id\":\"" + postIds.get(1) + "\",\"message\":\"second (edited)\",\"user_id\":\"someone-else\",\"is_pinned\":true}");
		assertThat(edit.status).isEqualTo(200);
		assertThat(edit.body.get("message").asString()).isEqualTo("second (edited)");
		assertThat(edit.body.get("edit_at").asLong()).isGreaterThan(0);
		assertThat(edit.body.get("is_pinned").asBoolean()).isFalse();
		// The recorded edit response has every post field except remote_id
		Set<String> recordedEditFields = new TreeSet<>(RECORDED_POST_FIELDS);
		recordedEditFields.remove("remote_id");
		assertThat(fieldNames(edit.body)).isEqualTo(recordedEditFields);

		// Delete the third message: it disappears from the list but shows up in "since" as deleted
		Reply delete = call("DELETE", "/api/v4/posts/" + postIds.get(2), token, null);
		assertThat(delete.status).isEqualTo(200);
		assertThat(delete.body.get("status").asString()).isEqualTo("OK");
		Reply afterDelete = call("GET", "/api/v4/channels/" + channelId + "/posts", token, null);
		assertThat(strings(afterDelete.body.get("order"))).containsExactly(postIds.get(1), postIds.get(0));
		Reply since = call("GET", "/api/v4/channels/" + channelId + "/posts?since=" + beforeChanges, token, null);
		assertThat(strings(since.body.get("order"))).containsExactlyInAnyOrder(postIds.get(2), postIds.get(1));
		assertThat(since.body.get("posts").get(postIds.get(2)).get("delete_at").asLong()).isGreaterThan(0);
		assertThat(call("DELETE", "/api/v4/posts/" + postIds.get(2), token, null).status).isEqualTo(404);

		// The channel counted the three messages
		Reply channelAfter = call("GET", "/api/v4/channels/" + channelId, token, null);
		assertThat(channelAfter.body.get("total_msg_count").asLong()).isEqualTo(3);
		assertThat(channelAfter.body.get("last_post_at").asLong()).isGreaterThan(0);
	}

	@Test
	void rejectsBadInputAndProtectsOtherPeoplesData() throws Exception {
		String owner = signUpAndLogin("owner", "owner@example.com");
		String outsider = signUpAndLogin("outsider", "outsider@example.com");

		// No token, wrong token, wrong password
		Reply noToken = call("GET", "/api/v4/users/me", null, null);
		assertThat(noToken.status).isEqualTo(401);
		assertThat(fieldNames(noToken.body)).containsExactlyInAnyOrder(
				"id", "message", "detailed_error", "request_id", "status_code");
		assertThat(call("GET", "/api/v4/users/me", "not-a-real-token", null).status).isEqualTo(401);
		assertThat(call("POST", "/api/v4/users/login", null,
				"{\"login_id\":\"owner\",\"password\":\"wrong-password\"}").status).isEqualTo(401);
		assertThat(call("POST", "/api/v4/users/login", null,
				"{\"login_id\":\"nobody\",\"password\":\"" + PASSWORD + "\"}").status).isEqualTo(401);

		// Duplicate and invalid sign-ups
		assertThat(call("POST", "/api/v4/users", null,
				"{\"email\":\"other@example.com\",\"username\":\"owner\",\"password\":\"" + PASSWORD + "\"}").status).isEqualTo(400);
		assertThat(call("POST", "/api/v4/users", null,
				"{\"email\":\"owner@example.com\",\"username\":\"owner2\",\"password\":\"" + PASSWORD + "\"}").status).isEqualTo(400);
		Reply weak = call("POST", "/api/v4/users", null, "{\"email\":\"not-an-email\",\"username\":\"x\",\"password\":\"short\"}");
		assertThat(weak.status).isEqualTo(400);
		assertThat(weak.body.get("detailed_error").asString()).contains("email", "username", "password");
		assertThat(call("POST", "/api/v4/users", null, "this is not json").status).isEqualTo(400);

		// Owner sets up a team, a channel and one message
		String teamId = call("POST", "/api/v4/teams", owner, "{\"name\":\"private-team\",\"display_name\":\"Private\"}")
				.body.get("id").asString();
		assertThat(call("POST", "/api/v4/teams", owner, "{\"name\":\"private-team\",\"display_name\":\"Again\"}").status).isEqualTo(400);
		String channelId = call("POST", "/api/v4/channels", owner,
				"{\"team_id\":\"" + teamId + "\",\"name\":\"plans\",\"display_name\":\"Plans\"}").body.get("id").asString();
		assertThat(call("POST", "/api/v4/channels", owner,
				"{\"team_id\":\"" + teamId + "\",\"name\":\"plans\",\"display_name\":\"Plans 2\"}").status).isEqualTo(400);
		String postId = call("POST", "/api/v4/posts", owner,
				"{\"channel_id\":\"" + channelId + "\",\"message\":\"secret plan\"}").body.get("id").asString();
		assertThat(call("POST", "/api/v4/posts", owner, "{\"channel_id\":\"" + channelId + "\",\"message\":\"  \"}").status).isEqualTo(400);
		assertThat(call("POST", "/api/v4/posts", owner,
				"{\"channel_id\":\"" + channelId + "\",\"message\":\"reply\",\"root_id\":\"doesnotexist\"}").status).isEqualTo(400);

		// The outsider is not in the team: cannot see, post, edit or delete
		assertThat(call("GET", "/api/v4/users/me/teams", outsider, null).body.size()).isEqualTo(0);
		assertThat(call("GET", "/api/v4/users/me/teams/" + teamId + "/channels", outsider, null).status).isEqualTo(403);
		assertThat(call("GET", "/api/v4/channels/" + channelId, outsider, null).status).isEqualTo(403);
		Reply read = call("GET", "/api/v4/channels/" + channelId + "/posts", outsider, null);
		assertThat(read.status).isEqualTo(403);
		assertThat(read.raw).doesNotContain("secret plan");
		assertThat(call("POST", "/api/v4/posts", outsider,
				"{\"channel_id\":\"" + channelId + "\",\"message\":\"hi\"}").status).isEqualTo(403);
		assertThat(call("POST", "/api/v4/channels", outsider,
				"{\"team_id\":\"" + teamId + "\",\"name\":\"mine\",\"display_name\":\"Mine\"}").status).isEqualTo(403);
		assertThat(call("PUT", "/api/v4/posts/" + postId + "/patch", outsider, "{\"message\":\"hacked\"}").status).isEqualTo(403);
		assertThat(call("DELETE", "/api/v4/posts/" + postId, outsider, null).status).isEqualTo(403);

		// Unknown ids
		assertThat(call("GET", "/api/v4/channels/doesnotexist", owner, null).status).isEqualTo(404);
		assertThat(call("PUT", "/api/v4/posts/doesnotexist/patch", owner, "{\"message\":\"x\"}").status).isEqualTo(404);

		// The message is untouched
		Reply list = call("GET", "/api/v4/channels/" + channelId + "/posts", owner, null);
		assertThat(list.body.get("posts").get(postId).get("message").asString()).isEqualTo("secret plan");
		assertThat(list.body.get("order").size()).isEqualTo(1);
	}

	@Test
	void invitedTeammateGetsLiveUpdatesAndOutsiderDoesNot() throws Exception {
		String alice = signUpAndLogin("alice", "alice@example.com");
		String bob = signUpAndLogin("bob", "bob@example.com");
		String carol = signUpAndLogin("carol", "carol@example.com");
		String aliceId = call("GET", "/api/v4/users/me", alice, null).body.get("id").asString();

		String teamId = call("POST", "/api/v4/teams", alice, "{\"name\":\"live-team\",\"display_name\":\"Live\"}")
				.body.get("id").asString();
		String channelId = call("POST", "/api/v4/channels", alice,
				"{\"team_id\":\"" + teamId + "\",\"name\":\"general\",\"display_name\":\"General\"}").body.get("id").asString();

		// Only a member may invite; unknown emails are reported per address, not as a failed call
		assertThat(call("POST", "/api/v4/teams/" + teamId + "/invite/email", carol, "[\"carol@example.com\"]").status).isEqualTo(403);
		assertThat(call("GET", "/api/v4/users/me/teams/" + teamId + "/channels", bob, null).status).isEqualTo(403);
		Reply invite = call("POST", "/api/v4/teams/" + teamId + "/invite/email", alice,
				"[\"Bob@Example.com\",\"nobody@example.com\"]");
		assertThat(invite.status).isEqualTo(200);
		assertThat(invite.body.get(0).get("email").asString()).isEqualTo("bob@example.com");
		assertThat(invite.body.get(0).get("error").isNull()).isTrue();
		assertThat(invite.body.get(1).get("error").get("id").asString()).isEqualTo("minichat.team.invite_no_account");
		assertThat(call("POST", "/api/v4/teams/" + teamId + "/invite/email", alice, "[\"bob@example.com\"]").status).isEqualTo(200);
		assertThat(call("POST", "/api/v4/teams/" + teamId + "/invite/email", alice, "[]").status).isEqualTo(400);

		// Bob is in; teammates see names but not email addresses
		assertThat(call("GET", "/api/v4/users/me/teams/" + teamId + "/channels", bob, null).status).isEqualTo(200);
		Reply members = call("GET", "/api/v4/users?in_team=" + teamId, bob, null);
		assertThat(members.status).isEqualTo(200);
		assertThat(strings(members.body, "username")).containsExactly("alice", "bob");
		assertThat(members.raw).doesNotContain("@example.com");
		assertThat(call("GET", "/api/v4/users?in_team=" + teamId, carol, null).status).isEqualTo(403);
		assertThat(call("GET", "/api/v4/users", bob, null).status).isEqualTo(400);

		// Live updates: a connection must prove who it is before it gets anything
		LiveClient stranger = LiveClient.connect(http, port);
		stranger.send("{\"action\":\"user_typing\",\"seq\":1,\"data\":{\"channel_id\":\"" + channelId + "\"}}");
		assertThat(stranger.next().get("status").asString()).isEqualTo("FAIL");
		LiveClient badToken = LiveClient.connect(http, port);
		badToken.send("{\"action\":\"authentication_challenge\",\"seq\":1,\"data\":{\"token\":\"not-a-real-token\"}}");
		assertThat(badToken.next().get("status").asString()).isEqualTo("FAIL");

		LiveClient bobLive = LiveClient.connect(http, port);
		bobLive.send("{\"action\":\"authentication_challenge\",\"seq\":1,\"data\":{\"token\":\"" + bob + "\"}}");
		JsonNode bobAuth = bobLive.next();
		assertThat(bobAuth.get("status").asString()).isEqualTo("OK");
		assertThat(bobAuth.get("seq_reply").asLong()).isEqualTo(1);
		LiveClient carolLive = LiveClient.connect(http, port);
		carolLive.send("{\"action\":\"authentication_challenge\",\"seq\":1,\"data\":{\"token\":\"" + carol + "\"}}");
		assertThat(carolLive.next().get("status").asString()).isEqualTo("OK");

		// Alice posts, edits and deletes over REST; Bob is told each time
		String postId = call("POST", "/api/v4/posts", alice,
				"{\"channel_id\":\"" + channelId + "\",\"message\":\"hello bob\"}").body.get("id").asString();
		JsonNode posted = bobLive.next();
		assertThat(posted.get("event").asString()).isEqualTo("posted");
		assertThat(posted.get("seq").asLong()).isEqualTo(1);
		assertThat(fieldNames(posted)).containsExactlyInAnyOrder("event", "data", "broadcast", "seq");
		assertThat(fieldNames(posted.get("data"))).containsExactlyInAnyOrder(
				"channel_display_name", "channel_name", "channel_type", "post", "sender_name", "set_online", "team_id");
		assertThat(fieldNames(posted.get("broadcast"))).containsExactlyInAnyOrder(
				"channel_id", "connection_id", "omit_connection_id", "omit_users", "team_id", "user_id");
		assertThat(posted.get("data").get("sender_name").asString()).isEqualTo("@alice");
		assertThat(posted.get("data").get("post").isString()).isTrue();
		JsonNode postedPost = JSON.readTree(posted.get("data").get("post").asString());
		assertThat(postedPost.get("id").asString()).isEqualTo(postId);
		assertThat(postedPost.get("message").asString()).isEqualTo("hello bob");

		call("PUT", "/api/v4/posts/" + postId + "/patch", alice, "{\"message\":\"hello bob!\"}");
		JsonNode edited = bobLive.next();
		assertThat(edited.get("event").asString()).isEqualTo("post_edited");
		assertThat(JSON.readTree(edited.get("data").get("post").asString()).get("message").asString()).isEqualTo("hello bob!");

		call("DELETE", "/api/v4/posts/" + postId, alice, null);
		JsonNode deleted = bobLive.next();
		assertThat(deleted.get("event").asString()).isEqualTo("post_deleted");
		assertThat(deleted.get("seq").asLong()).isEqualTo(3);
		assertThat(deleted.get("data").get("delete_by").asString()).isEqualTo(aliceId);
		assertThat(fieldNames(deleted.get("broadcast"))).containsExactlyInAnyOrder(
				"channel_id", "connection_id", "contains_sensitive_data", "omit_connection_id", "omit_users", "team_id", "user_id");

		// A failed post tells nobody, and Carol (not in the team) never heard anything
		assertThat(call("POST", "/api/v4/posts", carol, "{\"channel_id\":\"" + channelId + "\",\"message\":\"let me in\"}").status).isEqualTo(403);
		assertThat(bobLive.nothingWithin(300)).isTrue();
		assertThat(carolLive.nothingWithin(50)).isTrue();

		// Logging out ends the token for REST calls and for live updates
		assertThat(call("POST", "/api/v4/users/logout", bob, null).status).isEqualTo(200);
		assertThat(call("GET", "/api/v4/users/me", bob, null).status).isEqualTo(401);
		call("POST", "/api/v4/posts", alice, "{\"channel_id\":\"" + channelId + "\",\"message\":\"after bob left\"}");
		assertThat(bobLive.nothingWithin(300)).isTrue();

		bobLive.close();
		carolLive.close();
	}

	@Test
	void oldSessionsStopWorking() throws Exception {
		String token = signUpAndLogin("sleepy", "sleepy@example.com");
		assertThat(call("GET", "/api/v4/users/me", token, null).status).isEqualTo(200);

		// Pretend the login happened 31 days ago (the limit is 30)
		long thirtyOneDaysAgo = System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000;
		assertThat(jdbc.update("update sessions set create_at = ? where token = ?", thirtyOneDaysAgo, token)).isEqualTo(1);

		LiveClient live = LiveClient.connect(http, port);
		live.send("{\"action\":\"authentication_challenge\",\"seq\":1,\"data\":{\"token\":\"" + token + "\"}}");
		assertThat(live.next().get("status").asString()).isEqualTo("FAIL");
		Reply expired = call("GET", "/api/v4/users/me", token, null);
		assertThat(expired.status).isEqualTo(401);
		assertThat(expired.body.get("id").asString()).isEqualTo("minichat.session.invalid");
		assertThat(jdbc.queryForObject("select count(*) from sessions where token = ?", Integer.class, token)).isZero();

		// Logging in again gives a fresh token that works
		Reply again = call("POST", "/api/v4/users/login", null, "{\"login_id\":\"sleepy\",\"password\":\"" + PASSWORD + "\"}");
		assertThat(call("GET", "/api/v4/users/me", again.token, null).status).isEqualTo(200);
	}

	@Test
	void servesTheWebPage() throws Exception {
		HttpResponse<String> page = http.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(page.statusCode()).isEqualTo(200);
		assertThat(page.body()).contains("<title>Mini Chat</title>");
		for (String asset : List.of("/app.js", "/styles.css")) {
			HttpResponse<String> file = http.send(
					HttpRequest.newBuilder(URI.create("http://localhost:" + port + asset)).build(),
					HttpResponse.BodyHandlers.ofString());
			assertThat(file.statusCode()).as(asset).isEqualTo(200);
		}
	}

	private String signUpAndLogin(String username, String email) throws Exception {
		Reply signUp = call("POST", "/api/v4/users", null,
				"{\"email\":\"" + email + "\",\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
		assertThat(signUp.status).isEqualTo(201);
		Reply login = call("POST", "/api/v4/users/login", null,
				"{\"login_id\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
		assertThat(login.status).isEqualTo(200);
		return login.token;
	}

	private Reply call(String method, String path, String token, String jsonBody) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
		if (token != null) {
			request.header("Authorization", "Bearer " + token);
		}
		if (jsonBody == null) {
			request.method(method, HttpRequest.BodyPublishers.noBody());
		} else {
			request.header("Content-Type", "application/json");
			request.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
		}
		HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
		JsonNode body = response.body().isBlank() ? null : JSON.readTree(response.body());
		return new Reply(response.statusCode(), body, response.body(), response.headers().firstValue("Token").orElse(null));
	}

	private static Set<String> fieldNames(JsonNode node) {
		return new TreeSet<>(node.propertyNames());
	}

	private static List<String> strings(JsonNode array) {
		List<String> out = new ArrayList<>();
		array.forEach(item -> out.add(item.asString()));
		return out;
	}

	private static List<String> strings(JsonNode array, String field) {
		List<String> out = new ArrayList<>();
		array.forEach(item -> out.add(item.get(field).asString()));
		return out;
	}

	private record Reply(int status, JsonNode body, String raw, String token) {
	}

	/** Minimal WebSocket client that queues every complete text frame it receives. */
	private static final class LiveClient implements WebSocket.Listener {

		private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
		private final StringBuilder partial = new StringBuilder();
		private WebSocket socket;

		static LiveClient connect(HttpClient http, int port) throws Exception {
			LiveClient client = new LiveClient();
			client.socket = http.newWebSocketBuilder()
					.buildAsync(URI.create("ws://localhost:" + port + "/api/v4/websocket"), client)
					.get(5, TimeUnit.SECONDS);
			return client;
		}

		void send(String text) throws Exception {
			socket.sendText(text, true).get(5, TimeUnit.SECONDS);
		}

		JsonNode next() throws Exception {
			String frame = frames.poll(5, TimeUnit.SECONDS);
			assertThat(frame).as("expected a live-update frame within 5 seconds").isNotNull();
			return JSON.readTree(frame);
		}

		boolean nothingWithin(long millis) throws Exception {
			return frames.poll(millis, TimeUnit.MILLISECONDS) == null;
		}

		void close() {
			socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			partial.append(data);
			if (last) {
				frames.add(partial.toString());
				partial.setLength(0);
			}
			webSocket.request(1);
			return null;
		}
	}
}
