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