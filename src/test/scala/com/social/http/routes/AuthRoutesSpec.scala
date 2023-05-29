package com.social.http.routes

import cats.data.OptionT
import cats.effect.*
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.social.core.Auth
import com.social.domain.auth.{LoginInfo, NewPasswordInfo}
import com.social.domain.security.{Authenticator, JwtToken}
import com.social.domain.user.{NewUserInfo, User}
import com.social.domain.{auth, user}
import com.social.fixtures.{SecuredRouteFixture, UserFixture}
import org.http4s.{AuthScheme, Credentials, HttpRoutes, Method, Request, Status}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.uri
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.headers.Authorization
import org.scalatest.matchers.should.Matchers.shouldBe
import org.typelevel.ci.CIStringSyntax
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import concurrent.duration.*

class AuthRoutesSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with Http4sDsl[IO]
  with UserFixture
  with SecuredRouteFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val mockedAuth = new Auth[IO] {
    override def signUp(userInfo: user.NewUserInfo): IO[Option[user.User]] =
      if userInfo.email == adminEmail then IO.pure(Some(admin))
      else IO.pure(None)

    override def login(email: String, password: String): IO[Option[User]] =
      if email == personEmail && password == personPass then
        Some(person).pure[IO]
      else IO.pure(None)

    override def changePassword(email: String, passwordInfo: auth.NewPasswordInfo): IO[Either[String, Option[user.User]]] =
      if email == personEmail then
        if passwordInfo.oldPassword == personPass then IO.pure(Right(Some(person)))
        else IO.pure(Left("Invalid password"))
      else IO.pure(Right(None))

    override def delete(email: String): IO[Boolean] = IO.pure(true)

    override def sendPasswordRecoveryToken(email: String): IO[Unit] = ???

    override def recoverPasswordFromToken(email: String, token: String, newPassword: String): IO[Boolean] = ???
  }

  val authRoutes = AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

  "Auth routes" - {
    "should return a 401 unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"auth/login")
            .withEntity(LoginInfo(personEmail, "sadpass"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 Ok if login succeeds" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"auth/login")
            .withEntity(LoginInfo(personEmail, personPass))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return a 400 Ok bad request if the email is in the database" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"auth/users")
            .withEntity(newUserPerson)
        )
      } yield {
        response.status shouldBe Status.BadRequest
      }
    }

    "should return a 201 created if the user creation succeeds" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"auth/users")
            .withEntity(newUserAdmin)
        )
      } yield {
        response.status shouldBe Status.Created
      }
    }

    "should return a 200 ok if logging out with a valid jwt" in {
      for {
        jwtToken <- mockedAuthenticator.create(personEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return 401 unauthorized if logging out without token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"auth/logout")
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return 404 not found if the user changing pass doesnt exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(adminPass, "newpass"))
        )
      } yield {
        response.status shouldBe Status.NotFound
      }
    }

    "should return 403 forbidden if the user changing pass with incorrect old password" in {
      for {
        jwtToken <- mockedAuthenticator.create(personEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrongpass", "newpass"))
        )
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }

    "should return 401 unauthorized if user jwt is invalid when changing pass" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"auth/users/password")
            .withEntity(NewPasswordInfo(personPass, "newpass"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return 200 ok if changing password for user with valid jwt and pass" in {
      for {
        jwtToken <- mockedAuthenticator.create(personEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(personPass, "newpass"))
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return 401 unauthorized if a non admin trys to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(personEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"auth/users/admin@domain.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return 200 ok if an admin trys to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"auth/users/person@domain.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }
  }
}
