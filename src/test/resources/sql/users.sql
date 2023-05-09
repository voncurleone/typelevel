CREATE TABLE users(
    email text NOT NULL,
    handle text NOT NULL,
    hashedPass text NOT NULL,
    firstName text,
    lastName text,
    role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users(
    email,
    handle,
    hashedPass,
    firstName,
    lastName,
    role
) VALUES (
    'person@domain.com',
    'person',
    'password',
    'per',
    'son',
    'USER'
);

INSERT INTO users(
    email,
    handle,
    hashedPass,
    firstName,
    lastName,
    role
) VALUES (
    'admin@domain.com',
    'admin',
    'secure password',
    'ad',
    'min',
    'ADMIN'
);