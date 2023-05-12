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
    '$2a$10$0w0m2PIznul0M84rn6cRk.gUc28LrMJTHEM9UVzEMnjJdjtnrtE/C',
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
    '$2a$10$bdA2ei/ZxQM4MEOxg1ihp.98Utpne1jLou2J8Yl4rRrndJ8LzznU6',
    'ad',
    'min',
    'ADMIN'
);