CREATE DATABASE social;
\c social;

CREATE TABLE posts(
  id uuid DEFAULT gen_random_uuid(),
  date bigint NOT NULL,
  email text NOT NULL,
  text text NOT NULL,
  likes int NOT NULL,
  disLikes int NOT NULL,
  tags text[],
  image text,
  hidden boolean NOT NULL DEFAULT false
);

ALTER TABLE posts
ADD CONSTRAINT pk_posts PRIMARY KEY (id);

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