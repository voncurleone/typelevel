package com.social.playground

import cats.effect.*
import com.social.core.*
import com.social.domain.Post.PostInfo
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor

import scala.io.StdIn

object PostPlayground extends IOApp.Simple {

  val postgre: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:social",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val postInfo: PostInfo = PostInfo.minimal("post text", 5, 3)

  override def run: IO[Unit] = postgre.use { xa =>
    for {
      posts <- LivePosts[IO](xa)
      _ <- IO(println("ready next....")) *> IO(StdIn.readLine())
      id <- posts.create("bill@gmail.com", postInfo)
      _ <- IO(println("ready next....")) *> IO(StdIn.readLine())
      list <- posts.all()
      _ <- IO(println(s"All posts: $list.\n ready next....")) *> IO(StdIn.readLine())
      _ <- posts.update(id, postInfo.copy(text = "this is new post text"))
      newPost <- posts.find(id)
      _ <- IO(println(s"New post: $newPost.\n ready next....")) *> IO(StdIn.readLine())
      _ <- posts.delete(id)
      postsAfter <- posts.all()
      _ <- IO(println(s"All posts after delete: $postsAfter. ready next....")) *> IO(StdIn.readLine())
    } yield ()
  }
}
