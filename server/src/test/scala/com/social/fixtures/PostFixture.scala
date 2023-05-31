package com.social.fixtures

import cats.syntax.all.*
import com.social.domain.post.{Post, PostInfo}

import java.util.UUID

trait PostFixture {

  val NotFoundPostUuid: UUID = UUID.fromString("6ea79557-3112-4c84-a8f5-1d1e2c300948")

  val AwesomePostUuid: UUID = UUID.fromString("843df718-ec6e-4d49-9289-f799c0f40064")

  val AwesomePost: Post = Post(
    AwesomePostUuid,
    659186086L,
    "voncurleone@gmail.com",
    PostInfo(
      "Awesome post!",
      7,
      2,
      Some(List("tag1", "tag2")),
      Some("image?!?")
    ),
    false
  )

  val InvalidPost: Post = Post(
    null,
    42L,
    "email@gmail.com",
    PostInfo.empty,
    false
  )

  val UpdatedAwesomePost: Post = Post(
    AwesomePostUuid,
    659186086L,
    "voncurleone@gmail.com",
    PostInfo(
      "Updated",
      10,
      32,
      Some(List("tag1", "tag3")),
      None
    ),
    false
  )

  val NewPostInfo: PostInfo = PostInfo(
    "A new post.",
    35,
    3,
    None,
    Some("image!!!!!!")
  )


  val AwesomePostWithIdNotFound: Post = AwesomePost.copy(id = NotFoundPostUuid)//AwesomeJob.copy(id = NotFoundJobUuid)

  val AnotherAwesomePostUuid: UUID = UUID.fromString("19a941d0-aa19-477b-9ab0-a7033ae65c2b")
  val AnotherAwesomePost: Post = AwesomePost.copy(id = AnotherAwesomePostUuid)

  val TestAwesomePost: Post =
    AwesomePost.copy(postInfo = AwesomePost.postInfo.copy(text = "Test"))

  val NewPostUuid: UUID = UUID.fromString("efcd2a64-4463-453a-ada8-b1bae1db4377")
  val AwesomeNewPostInfo: PostInfo = PostInfo(
    "New awesome post!",
    67,
    87,
    None,
    None
  )
}