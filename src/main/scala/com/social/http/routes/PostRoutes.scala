package com.social.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.social.core.Posts
import com.social.domain.Post.{Post, PostInfo}
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*

import java.util.UUID
import scala.collection.mutable
import org.typelevel.log4cats.Logger
import com.social.logging.Syntax.*

import com.social.http.validation.Syntax.*

//uuid => 11111111-1111-1111-1111-111111111111
class PostRoutes[F[_] : Concurrent: Logger] private (posts: Posts[F]) extends HttpValidationDsl[F] {

  //POST /posts?offset=x&limit=y { filters } //todo: add query params and filters
  private val allPostsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root => for {
      postList <- posts.all()
      response <- Ok(postList)
    } yield response
  }

  //GET /posts/uuid
  private val findPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      posts.find(id).flatMap {
        case Some(post) => Ok(post)
        case None => NotFound(s"Post id $id not found.")
      }.logError( e => s"Error getting post: $e")
  }


  //POST /posts { jobInfo }
  private val createPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "create" =>
      request.validate[PostInfo] { postInfo =>
        for {
          _ <- Logger[F].info("Creating post: simple log example")

          postId <- posts.create("todo@todo.com", postInfo).log(
            a => s"Creating post: $a",
            e => s"Error creating post: $e"
          )
          response <- Created(postId)
        } yield response
      }
  }

  //PUT /posts/uuid { jobInfo }
  private val updatePostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ PUT -> Root / UUIDVar(id) =>
      request.validate[PostInfo] { postInfo =>
        for {
          newPost <- posts.update(id, postInfo) //.logError(e => s"Error updating post: $e")
          response <- newPost match
            case Some(post) => Ok()
            case None => NotFound(s"Can't update post $id: post not found.")
        } yield response
      }
  }

  //DELETE /posts/uuid
  private val deletePostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) => posts.find(id).flatMap {
      case Some(_) => for {
        _ <- posts.delete(id)
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
  def apply[F[_]: Concurrent: Logger](posts: Posts[F]) = new PostRoutes[F](posts)
}
