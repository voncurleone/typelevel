package com.social.http.routes

import cats.effect.*
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.social.core.Posts
import com.social.domain.pagination.Pagination
import com.social.domain.post.{Post, PostFilter, PostInfo}
import com.social.fixtures.PostFixture
import org.http4s.{HttpRoutes, Method, Request, Status}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.uri
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.scalatest.matchers.should.Matchers.shouldBe

import java.util.UUID

class PostRoutesSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with Http4sDsl[IO]
  with PostFixture {

  //Prep
  //Mock the Posts module
  val posts: Posts[IO] = new Posts[IO] {
    def create(ownerEmail: String, postInfo: PostInfo): IO[UUID] = IO.pure(NewPostUuid)

    def all(): IO[List[Post]] = IO.pure(List(AwesomePost))

    def all(filters: PostFilter, pagination: Pagination): IO[List[Post]] =
      if filters.hidden then IO.pure(List())
      else IO.pure(List(AwesomePost))

    def find(id: UUID): IO[Option[Post]] =
      if id == AwesomePostUuid then IO.pure(Some(AwesomePost))
      else IO.pure(None)

    def update(id: UUID, postInfo: PostInfo): IO[Option[Post]] =
      if id == AwesomePostUuid then IO.pure(Some(UpdatedAwesomePost))
      else IO.pure(None)

    def hide(id: UUID): IO[Int] =
      if id == AwesomePostUuid then IO.pure(1)
      else IO.pure(0)

    def delete(id: UUID): IO[Int] =
      if id == AwesomePostUuid then IO.pure(1)
      else IO.pure(0)
  }

  //build jobsRoutes
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  val postRoutes: HttpRoutes[IO] = PostRoutes[IO](posts).routes

  //Tests
  "PostRoutes" - {
    "should return a post with a given id" in {
      for {
        //simulate an http request
        response <- postRoutes.orNotFound.run {
          Request(method = Method.GET, uri = uri"/posts/843df718-ec6e-4d49-9289-f799c0f40064")
        }

        retrieved <- response.as[Post]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomePost
      }
    }

    "should return all posts" in {
      for {
        //simulate an http request
        response <- postRoutes.orNotFound.run {
          Request(method = Method.POST, uri = uri"/posts")
            .withEntity(PostFilter())
        }

        retrieved <- response.as[List[Post]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomePost)
      }
    }

    "should return all posts that satisfy a filter" in {
      for {
        //simulate an http request
        response <- postRoutes.orNotFound.run {
          Request(method = Method.POST, uri = uri"/posts")
            .withEntity(PostFilter(hidden = true))
        }

        retrieved <- response.as[List[Post]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List()
      }
    }

    "should create a new post" in {
      for {
        //simulate an http request
        response <- postRoutes.orNotFound.run {
          Request(method = Method.POST, uri = uri"/posts/create")
            .withEntity(AwesomePost.postInfo)
        }

        retrieved <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe NewPostUuid
      }
    }

    "should only update a post that exists" in {
      for {
        //simulate an http request
        responseOk <- postRoutes.orNotFound.run {
          Request(method = Method.PUT, uri = uri"/posts/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomePost.postInfo)
        }

        response404 <- postRoutes.orNotFound.run {
          Request(method = Method.PUT, uri = uri"/posts/843df718-ec6e-4d49-9289-f799c0f40000")
            .withEntity(UpdatedAwesomePost.postInfo)
        }
      } yield {
        responseOk.status shouldBe Status.Ok
        response404.status shouldBe Status.NotFound
      }
    }

    "should only delete a post that exists" in {
      for {
        //simulate an http request
        responseOk <- postRoutes.orNotFound.run {
          Request(method = Method.DELETE, uri = uri"/posts/843df718-ec6e-4d49-9289-f799c0f40064")
        }

        response404 <- postRoutes.orNotFound.run {
          Request(method = Method.DELETE, uri = uri"/posts/843df718-ec6e-4d49-9289-f799c0f40000")
        }
      } yield {
        responseOk.status shouldBe Status.Ok
        response404.status shouldBe Status.NotFound
      }
    }
  }
}
