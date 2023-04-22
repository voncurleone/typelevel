package com.social.http.routes

import cats.*
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*

//uuid => 11111111-1111-1111-1111-111111111111
class PostRoutes[F[_] : Monad] private extends Http4sDsl[F] {

  //POST /posts?offset=x&limit=y { filters } //todo: add query params and filters
  private val allPostsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root => Ok("TODO")
  }

  //GET /posts/uuid
  private val findPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) => Ok(s"TODO: find post: $id")
  }

  //POST /posts { jobInfo }
  private val createPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "create" => Ok("TODO")
  }

  //PUT /posts/uuid { jobInfo }
  private val updatePostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case PUT -> Root / UUIDVar(id) => Ok(s"TODO update post: $id")
  }

  //DELETE /posts/uuid
  private val deletePostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) => Ok(s"TODO delete post: $id")
  }

  val routes: HttpRoutes[F] = Router(
    "/posts" -> (allPostsRoute <+> findPostRoute <+> createPostRoute <+> updatePostRoute <+> deletePostRoute)
  )
}

object PostRoutes {
  def apply[F[_]: Monad] = new PostRoutes[F]
}
