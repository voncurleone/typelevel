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

INSERT INTO posts(
    id,
    date,
    email,
    text,
    likes,
    disLikes,
    tags,
    image,
    hidden
) VALUES (
     '843df718-ec6e-4d49-9289-f799c0f40064',
     659186086,
     'voncurleone@gmail.com',
     'Awesome post!',
     7,
     2,
     ARRAY ['tag1', 'tag2'],
     'image?!?',
     false
 );