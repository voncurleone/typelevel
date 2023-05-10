package com.social.core

import cats.data.OptionT
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.social.domain.auth.NewPasswordInfo
import com.social.domain.security.*
import com.social.domain.user.*
import com.social.fixtures.UserFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.{PasswordHash, jca}
import tsec.passwordhashers.jca.BCrypt

import concurrent.duration.*

class AuthSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with UserFixture {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedUsers: Users[IO] = new Users[IO] {
    override def create(user: User): IO[Option[String]] = IO.pure(Some(user.email))

    override def find(email: String): IO[Option[User]] =
      if(email == personEmail) IO(Some(person))
      else IO(None)

    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }

  val mockedAuthenticator: Authenticator[IO] = {
    //key for hashing
    val key = HMACSHA256.unsafeGenerateKey

    //identity store to retrieve users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if(email == personEmail) OptionT.pure(person)
      else if(email == adminEmail) OptionT.pure(admin)
      else OptionT.none[IO, User]

    //jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken (
      //expiry of token, max idle optional, idStore, key
      1.day,
      None,
      idStore,
      key
    )
  }

  "Auth algebra" - {
    "login should return none if the user does not exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        tokenOption <- auth.login("notvalid@domain.com", "pass")
      } yield (tokenOption)

      program.asserting(_ shouldBe None)
    }

    "login should return none if the user exists and the password is incorrect" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        tokenOption <- auth.login(personEmail, "pass")
      } yield (tokenOption)

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        tokenOption <- auth.login(personEmail, "password")
      } yield (tokenOption)

      program.asserting(_ shouldBe defined)// defined == Some(???)
    }

    "signup should not create a user if the email is already registered" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        userOption <- auth.signUp(NewUserInfo(personEmail, "handle", "pass", None, None))
      } yield (userOption)

      program.asserting(_ shouldBe None)
    }

    "signup should create a user if the email is not already registered" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        userOption <- auth.signUp(NewUserInfo(newUser.email, "handle", "pass", None, None))
      } yield (userOption)

      program.asserting {
        case Some(user) =>
          user.email shouldBe newUser.email
          user.handle shouldBe "handle"
          user.firstName shouldBe None
          user.lastName shouldBe None
          user.role shouldBe Role.USER

        case _ => fail()
      }
    }

    "change password should return right(none) if the user doesnt exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword("notvalid@domain.com", NewPasswordInfo("old", "new"))
      } yield (result)

      program.asserting(_ shouldBe Right(None))
    }

    "change password should return left with error if the password is incorrect" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword(personEmail, NewPasswordInfo("old", "new"))
      } yield (result)

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "change password should correctly change password if all info is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword(personEmail, NewPasswordInfo("password", "newpassword"))
        isValidPass <- result match {
          case Right(Some(user)) =>
            BCrypt.checkpwBool[IO]("newpassword", PasswordHash[BCrypt](user.hashedPass))

          case _ => IO.pure(false)
        }
      } yield (isValidPass)

      program.asserting(_ shouldBe true)
    }
  }
}
