CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    username VARCHAR(50) NOT NULL UNIQUE
    CONSTRAINT chk_username_format
    CHECK (username ~ '^[a-zA-Z][a-zA-Z0-9_]{2,49}$'),

    password_hash TEXT NOT NULL
    CONSTRAINT chk_password_hash
    CHECK (LENGTH(TRIM(password_hash)) > 0),

    email VARCHAR(100) NOT NULL UNIQUE
    CONSTRAINT chk_email_format
    CHECK (
        email ~* '^[a-z0-9][a-z0-9._%+-]*@[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$'
        AND email !~ '\.\.'
    ),

    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER'
    CONSTRAINT chk_user_role
    CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE TABLE posts (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    content TEXT,
    author_id   BIGINT NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    text       VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    post_id    BIGINT NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    author_id  BIGINT NOT NULL REFERENCES users (id)
);