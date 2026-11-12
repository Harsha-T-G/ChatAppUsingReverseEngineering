# REA mini chat backend

A small Spring Boot chat backend that was built from a recording, not from source code.

[REA](https://github.com/morluto/rea) watched a Chrome tab while a person used a locally running chat
server (Mattermost, the open-source Slack alternative). It wrote down every API call the page made:
the method, the path, the status, and the names and types of the fields. This backend was then written
from that list.

## How it was made

1. Ran the chat server locally in Docker and opened it in a debug-mode Chrome window.
2. Started `rea inspect-web-page` with body shapes on, then signed up, made a team and a channel,
   and sent, edited and deleted messages.
3. REA recorded 248 requests covering 57 distinct endpoints. The result is in
   [docs/blueprint.md](docs/blueprint.md).
4. Picked the 12 calls that make up the chat ([docs/rea-contract.json](docs/rea-contract.json))
   and built them here.

REA records field names and types only. It never records values, headers or cookies, so no
password or message text was captured.

## The 12 calls

| What it does | Call |
|---|---|
| Sign up | `POST /api/v4/users` |
| Log in | `POST /api/v4/users/login` |
| Who am I | `GET /api/v4/users/me` |
| Create team | `POST /api/v4/teams` |
| My teams | `GET /api/v4/users/me/teams` |
| Create channel | `POST /api/v4/channels` |
| My channels in a team | `GET /api/v4/users/me/teams/{teamId}/channels` |
| Open a channel | `GET /api/v4/channels/{channelId}` |
| Send message | `POST /api/v4/posts` |
| Read messages | `GET /api/v4/channels/{channelId}/posts` |
| Edit message | `PUT /api/v4/posts/{postId}/patch` |
| Delete message | `DELETE /api/v4/posts/{postId}` |

Built on top of those, also from the recording:

| What it does | Call | Difference from the original |
|---|---|---|
| Live updates | WebSocket `/api/v4/websocket` | Same frame shapes; sends `posted`, `post_edited` and `post_deleted` |
| Add a person to a team | `POST /api/v4/teams/{teamId}/invite/email` | The original emails an invitation; this one adds people who already have an account |
| List teammates | `GET /api/v4/users?in_team={teamId}` | Only for your own teams, and without email addresses |

## Run it

You need Java 25 and Docker.

```bash
docker compose up -d        # Postgres on port 5440
./mvnw spring-boot:run      # backend on http://localhost:8090
```

- Chat page: <http://localhost:8090/>
- Swagger UI: <http://localhost:8090/swagger-ui.html>

### Two people chatting (good for a demo)

1. Open <http://localhost:8090/> and sign up as the first person.
2. Create a team with **+ New team**, then a channel with the **+** next to Channels.
3. Open the same address in a second tab or window and sign up as a second person.
   Each tab keeps its own login, so two tabs can be two different people.
4. In the first tab, click the **+** next to People and enter the second person's email.
5. Reload the second tab. Both people are now in the channel, and messages, edits and
   deletes show up on the other side without reloading.

### By hand in Swagger

1. Call `POST /api/v4/users` to sign up.
2. Call `POST /api/v4/users/login` with `login_id` (your email or username) and `password`.
3. Copy the `Token` value from the response headers, click **Authorize**, and paste it.
4. Create a team, then a channel (use the team's `id` as `team_id`), then send messages.

## Compare it with the recording

With the backend running:

```bash
python3 scripts/demo.py
```

This replays the 12 calls and checks each response against what REA recorded. Current result:
all 12 calls return the recorded status, and 146 of the 211 recorded response fields are returned.
The script lists the fields this mini version leaves out.

## Tests

```bash
./mvnw test
```

The tests start the real application with a real Postgres (Testcontainers, so Docker must be running)
and cover:

- the full recorded flow, from sign-up to deleting a message;
- failure cases: wrong password, duplicate names, invalid input;
- privacy: a person outside a team cannot read, post, edit, delete, invite or list its members;
- live updates: a teammate receives new, edited and deleted messages; an outsider, an unauthenticated
  connection and a logged-out token receive nothing.

## What came from where

| Source | Examples |
|---|---|
| **Observed by REA** | Paths, methods, status codes, field names and types, the error body (`id`, `message`, `detailed_error`, `request_id`, `status_code`), ids being 26 lowercase letters and digits, the `since` query parameter |
| **Inferred from the recording** | Timestamps are epoch milliseconds; `delete_at` means deletes keep the row; `order` lists post ids newest first |
| **My own design (REA cannot see it)** | Login token in a `Token` response header and `Authorization: Bearer` on later calls; BCrypt password hashing; who is allowed to do what; validation rules; the table design; the type code `"O"` |

Additions that were not in the recording:

- `login_id` on the login call. The recording only contains the automatic login right after sign-up,
  which sends the user `id`. People log in with an email or username, so that was added.
- `page` and `per_page` on the message list. They were recorded on other list calls and are reused.
- `in_team` on the user list, so it can be limited to your own team.
- `POST /api/v4/users/logout`. Nobody logged out while REA was watching, so it had to be added.
- How a WebSocket connection proves who it is. The browser did it with a cookie, which REA does not
  record. Here the first frame must be
  `{"action": "authentication_challenge", "seq": 1, "data": {"token": "<login token>"}}`.
- The names of the live events (`posted`, `post_edited`, `post_deleted`). REA recorded that an `event`
  field exists and that `data.post` is a string holding the post as JSON, but not the values.

## Not built

- Threads, files, pinning, reactions, direct messages, typing indicators, unread counts on the server.
- Leaving a team or removing someone from it.
- Sessions do not expire by themselves; a token works until that person logs out.

## Code layout

| Package | Role |
|---|---|
| `user`, `team`, `channel`, `post` | One folder per feature: controller, service, repository, entity, `dto` records |
| `auth` | `AuthInterceptor` checks the token on every call except sign-up and login |
| `realtime` | WebSocket handler; sends an event only after the database change has committed |
| `common` | Id generator and the error handler |
| `src/main/resources/db/migration` | Flyway SQL that creates the tables |
| `src/main/resources/static` | The chat page: plain HTML, CSS and JavaScript, no build step |
