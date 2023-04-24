package com.social.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.social.domain.Post.{Post, PostInfo}
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*

import java.util.UUID
import scala.collection.mutable

import org.typelevel.log4cats.Logger
import com.social.logging.syntax.*

//uuid => 11111111-1111-1111-1111-111111111111
class PostRoutes[F[_] : Concurrent: Logger] private extends Http4sDsl[F] {

  //Database
  private val database = mutable.Map[UUID, Post]()

  //POST /posts?offset=x&limit=y { filters } //todo: add query params and filters
  private val allPostsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root => Ok(database.values)
  }

  //GET /posts/uuid
  private val findPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) => database.get(id) match {
      case Some(post) => Ok(post)
      case None => NotFound(s"Post id $id not found.")
    }
  }

  private def createPost(postInfo: PostInfo): F[Post] = {
    Post(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      email = "todo@todo.com",
      postInfo = postInfo,
      hidden = false
    ).pure[F]
  }

  //POST /posts { jobInfo }
  private val createPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "create" => for {
      _ <- Logger[F].info("Creating post: simple log example")

      postInfo <- request.as[PostInfo].log(
        a => s"Creating post: $a",
        e => s"Error creating post: $e"
      )

      post <- createPost(postInfo)
      _ <- database.put(post.id, post).pure[F]
      response <- Created(post.id)
    } yield response
  }

  //PUT /posts/uuid { jobInfo }
  private val updatePostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ PUT -> Root / UUIDVar(id) => database.get(id) match {
      case Some(post) => for {
        postInfo <- request.as[PostInfo]
        _ <- database.put(id, post.copy(postInfo = postInfo)).pure[F]
        response <- Ok()
      } yield response

      case None => NotFound(s"Can't update post $id: post not found.")
    }
  }

  //DELETE /posts/uuid
  private val deletePostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) => database.get(id) match {
      case Some(_) => for {
        _ <- database.remove(id).pure[F]
        response <- Ok()
      } yield response

      case None => NotFound(s"Can't delete post $id: post not found.")
    }
  }

  val routes: HttpRoutes[F] = Router(
    "/posts" -> (allPostsRoute <+> findPostRoute <+> createPostRoute <+> updatePostRoute <+> deletePostRoute)
  )
}

object PostRoutes {
  def apply[F[_]: Concurrent: Logger] = new PostRoutes[F]
}
