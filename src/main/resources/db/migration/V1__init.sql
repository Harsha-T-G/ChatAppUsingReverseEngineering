-- Table design derived from the field names and types REA recorded (see docs/blueprint.md).
-- Observed: ids are strings, timestamps are numbers, deletes keep the row (delete_at).
-- Inferred: timestamps are epoch milliseconds, delete_at = 0 means "not deleted".

CREATE TABLE users (
    id            VARCHAR(26)  PRIMARY KEY,
    create_at     BIGINT       NOT NULL,
    update_at     BIGINT       NOT NULL,
    delete_at     BIGINT       NOT NULL DEFAULT 0,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    email         VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(64)  NOT NULL DEFAULT '',
    last_name     VARCHAR(64)  NOT NULL DEFAULT '',
    nickname      VARCHAR(64)  NOT NULL DEFAULT '',
    roles         VARCHAR(256) NOT NULL,
    locale        VARCHAR(8)   NOT NULL DEFAULT 'en'
);

-- Not observed by REA (it never records headers or cookies): how the login token is kept.
CREATE TABLE sessions (
    token     VARCHAR(26) PRIMARY KEY,
    user_id   VARCHAR(26) NOT NULL REFERENCES users (id),
    create_at BIGINT      NOT NULL
);

CREATE TABLE teams (
    id                VARCHAR(26)  PRIMARY KEY,
    create_at         BIGINT       NOT NULL,
    update_at         BIGINT       NOT NULL,
    delete_at         BIGINT       NOT NULL DEFAULT 0,
    name              VARCHAR(64)  NOT NULL UNIQUE,
    display_name      VARCHAR(64)  NOT NULL,
    description       VARCHAR(255) NOT NULL DEFAULT '',
    email             VARCHAR(128) NOT NULL DEFAULT '',
    type              VARCHAR(8)   NOT NULL,
    company_name      VARCHAR(64)  NOT NULL DEFAULT '',
    allowed_domains   VARCHAR(255) NOT NULL DEFAULT '',
    invite_id         VARCHAR(26)  NOT NULL,
    allow_open_invite BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE team_members (
    id        VARCHAR(26) PRIMARY KEY,
    team_id   VARCHAR(26) NOT NULL REFERENCES teams (id),
    user_id   VARCHAR(26) NOT NULL REFERENCES users (id),
    roles     VARCHAR(64) NOT NULL,
    create_at BIGINT      NOT NULL,
    UNIQUE (team_id, user_id)
);

CREATE TABLE channels (
    id              VARCHAR(26)   PRIMARY KEY,
    create_at       BIGINT        NOT NULL,
    update_at       BIGINT        NOT NULL,
    delete_at       BIGINT        NOT NULL DEFAULT 0,
    team_id         VARCHAR(26)   NOT NULL REFERENCES teams (id),
    type            VARCHAR(8)    NOT NULL,
    name            VARCHAR(64)   NOT NULL,
    display_name    VARCHAR(64)   NOT NULL,
    header          VARCHAR(1024) NOT NULL DEFAULT '',
    purpose         VARCHAR(250)  NOT NULL DEFAULT '',
    creator_id      VARCHAR(26)   NOT NULL REFERENCES users (id),
    last_post_at    BIGINT        NOT NULL DEFAULT 0,
    total_msg_count BIGINT        NOT NULL DEFAULT 0,
    UNIQUE (team_id, name)
);

CREATE TABLE posts (
    id              VARCHAR(26) PRIMARY KEY,
    create_at       BIGINT      NOT NULL,
    update_at       BIGINT      NOT NULL,
    edit_at         BIGINT      NOT NULL DEFAULT 0,
    delete_at       BIGINT      NOT NULL DEFAULT 0,
    user_id         VARCHAR(26) NOT NULL REFERENCES users (id),
    channel_id      VARCHAR(26) NOT NULL REFERENCES channels (id),
    root_id         VARCHAR(26) NOT NULL DEFAULT '',
    message         TEXT        NOT NULL,
    pending_post_id VARCHAR(64) NOT NULL DEFAULT ''
);

CREATE INDEX idx_posts_channel_created ON posts (channel_id, create_at DESC);
