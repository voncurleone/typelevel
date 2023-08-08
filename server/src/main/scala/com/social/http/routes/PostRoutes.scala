package com.social.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.social.core.Posts
import com.social.domain.pagination.Pagination
import com.social.domain.post.{Post, PostFilter, PostInfo}
import com.social.domain.security.{AuthRoute, Authenticator, JwtToken, SecuredHandler, allRoles, registeredOnly, restrictedTo}
import com.social.domain.user.User
import com.social.http.responses.Responses.FailureResponse
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*

import java.util.UUID
import scala.collection.mutable
import org.typelevel.log4cats.Logger
import com.social.logging.Syntax.*
import com.social.http.validation.Syntax.*
import tsec.authentication.{SecuredRequestHandler, asAuthed}

//uuid => 11111111-1111-1111-1111-111111111111
class PostRoutes[F[_] : Concurrent: Logger: SecuredHandler] private (posts: Posts[F])
  extends HttpValidationDsl[F] {

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")

  //POST /posts?limit=x&offset=y { filters } 
  private val allPostsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) => for {
      filters <- request.as[PostFilter]
      postList <- posts.all(filters, Pagination(limit, offset))
      response <- Ok(postList)
    } yield response
  }

  //GET /posts/filters
  private val filtersRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "filters" =>
      posts.filters().flatMap( filter => Ok(filter))
  }

  //GET /posts/uuid
  private val findPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      posts.find(id).flatMap {
        case Some(post) => Ok(post)
        case None => NotFound(s"Post id $id not found.")
      }.logError( e => s"Error getting post: $e")
  }


  //POST /posts/create { jobInfo }
  private val createPostRoute: AuthRoute[F] = {
    case request @ POST -> Root / "create" asAuthed user =>
      request.request.validate[PostInfo] { postInfo =>
        for {
          _ <- Logger[F].info("Creating post: simple log example")
          postId <- posts.create(user.email, postInfo).log(
            a => s"Creating post: $a",
            e => s"Error creating post: $e"
          )
          response <- Created(postId)
        } yield response
      }
  }

  //PUT /posts/uuid { jobInfo }
  private val updatePostRoute: AuthRoute[F] = {
    case request @ PUT -> Root / UUIDVar(id) asAuthed user =>
      request.request.validate[PostInfo] { postInfo =>
        posts.find(id).flatMap {
          case None => NotFound(FailureResponse("cant delete a post that is not yours"))
          case Some(post) =>
            posts.update(id, postInfo) *> Ok()
        }
      }
  }

  //DELETE /posts/uuid
  private val deletePostRoute: AuthRoute[F] = {
    case DELETE -> Root / UUIDVar(id) asAuthed user => posts.find(id).flatMap {
      case Some(post) if user.owns(post) || user.isAdmin =>
        posts.delete(id) *> Ok()

      case None => NotFound(s"Can't delete post $id: post not found.")
      case _ => Forbidden(FailureResponse("you can only delete your own posts"))
    }
  }

  val authedRoutes = SecuredHandler[F].liftService(
    createPostRoute.restrictedTo(allRoles) |+|
      deletePostRoute.restrictedTo(registeredOnly) |+|
      updatePostRoute.restrictedTo(registeredOnly)
  )
  val unauthedRotes = allPostsRoute <+> findPostRoute <+> filtersRoute
  val routes: HttpRoutes[F] = Router(
    "/posts" -> (unauthedRotes <+> authedRoutes)
  )
}

object PostRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](posts: Posts[F]) =
    new PostRoutes[F](posts)
}
