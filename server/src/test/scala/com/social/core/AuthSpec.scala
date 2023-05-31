package com.social.core

import cats.data.OptionT
import cats.effect.{IO, Ref}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.social.config.SecurityConfig
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

  val mockedConfig = SecurityConfig("secret", 1.day)

  val mockedTokens: Tokens[IO] = new Tokens[IO] {
    override def getToken(email: String): IO[Option[String]] =
      if email == personEmail then IO(Some("abc123"))
      else IO.pure(None)

    override def checkToken(email: String, token: String): IO[Boolean] =
      IO.pure(token == "abc123")
  }

  val mockedEmails: Emails[IO] = new Emails[IO] {
    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] = IO.unit

    override def sendEmail(to: String, subject: String, content: String): IO[Unit] = IO.unit
  }

  def probeEmails(users: Ref[IO, Set[String]]): Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
      users.modify(set => (set + to, ()))
    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] =
      sendEmail(to, "yourToken", "token")
  }

  "Auth algebra" - {
    "login should return none if the user does not exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        tokenOption <- auth.login("notvalid@domain.com", "pass")
      } yield (tokenOption)

      program.asserting(_ shouldBe None)
    }

    "login should return none if the user exists and the password is incorrect" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        tokenOption <- auth.login(personEmail, "pass")
      } yield (tokenOption)

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        tokenOption <- auth.login(personEmail, "password")
      } yield (tokenOption)

      program.asserting(_ shouldBe defined)// defined == Some(???)
    }

    "signup should not create a user if the email is already registered" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        userOption <- auth.signUp(NewUserInfo(personEmail, "handle", "pass", None, None))
      } yield (userOption)

      program.asserting(_ shouldBe None)
    }

    "signup should create a user if the email is not already registered" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
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
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword("notvalid@domain.com", NewPasswordInfo("old", "new"))
      } yield (result)

      program.asserting(_ shouldBe Right(None))
    }

    "change password should return left with error if the password is incorrect" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(personEmail, NewPasswordInfo("old", "new"))
      } yield (result)

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "change password should correctly change password if all info is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(personEmail, NewPasswordInfo("password", "newpassword"))
        isValidPass <- result match {
          case Right(Some(user)) =>
            BCrypt.checkpwBool[IO]("newpassword", PasswordHash[BCrypt](user.hashedPass))

          case _ => IO.pure(false)
        }
      } yield (isValidPass)

      program.asserting(_ shouldBe true)
    }

    "recover password should fail for a user that does not exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result1 <- auth.recoverPasswordFromToken("someone@domain.com", "abc123", "newpass")
        result2 <- auth.recoverPasswordFromToken("someone@domain.com", "wrong", "newpass")
      } yield (result1, result2)

      program.asserting(_ shouldBe (false, false))
    }

    "recover password should fail for a user does exist but the token is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(personEmail, "wrong", "newpass")
      } yield (result)

      program.asserting(_ shouldBe false)
    }

    "recover password should succeed for a correct combo user token" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(personEmail, "abc123", "newpass")
      } yield (result)

      program.asserting(_ shouldBe true)
    }

    "sending recovery passwords should fail for a user that doesnt exist" in {
      val program = for {
        set <- Ref.of[IO, Set[String]](Set())
        emails <- IO(probeEmails(set))
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        _ <- auth.sendPasswordRecoveryToken("someone@domain.com")
        usersSentEmails <- set.get
      } yield (usersSentEmails)

      program.asserting(_ shouldBe empty)
    }

    "sending recovery passwords should succeed for a user that exists" in {
      val program = for {
        set <- Ref.of[IO, Set[String]](Set())
        emails <- IO(probeEmails(set))
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        _ <- auth.sendPasswordRecoveryToken(personEmail)
        usersSentEmails <- set.get
      } yield (usersSentEmails)

      program.asserting(_ should contain(personEmail))
    }
  }
}
