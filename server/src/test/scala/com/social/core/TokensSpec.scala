package com.social.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.social.config.TokenConfig
import com.social.fixtures.{PostFixture, UserFixture}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import concurrent.duration.DurationInt

class TokensSpec extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with DoobieSpec
  with UserFixture {
  override val initScript: String = "sql/tokens.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Token algebra" - {
    "should not create a new token for a non existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100000000L))
          token <- tokens.getToken("somebody@domain.com")
        } yield token

        program.asserting(_ shouldBe None)
      }
    }

    "should create a token for an existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100000000L))
          token <- tokens.getToken(personEmail)
        } yield token

        program.asserting(_ shouldBe defined)
      }
    }

    "should not validate expired tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100L))
          tokenOption <- tokens.getToken(personEmail)
          _ <- IO.sleep(500.millis)
          isValidToken <- tokenOption match {
            case Some(token) => tokens.checkToken(personEmail, token)
            case None => IO.pure(false)
          }
        } yield isValidToken

        program.asserting(_ shouldBe false)
      }
    }

    "should not validate non expired tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(1000000L))
          tokenOption <- tokens.getToken(personEmail)
          isValidToken <- tokenOption match {
            case Some(token) => tokens.checkToken(personEmail, token)
            case None => IO.pure(false)
          }
        } yield isValidToken

        program.asserting(_ shouldBe true)
      }
    }

    "should only validate tokens for the user that generated them" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100L))
          tokenOption <- tokens.getToken(personEmail)
          isValidTokenPerson <- tokenOption match {
            case Some(token) => tokens.checkToken(personEmail, token)
            case None => IO.pure(false)
          }
          isValidTokenOther <- tokenOption match {
            case Some(token) => tokens.checkToken("someone@domain.com", token)
            case None => IO.pure(false)
          }
        } yield (isValidTokenPerson, isValidTokenOther)

        program.asserting {
          case (personToken, otherToken) =>
            personToken shouldBe true
            otherToken shouldBe false
        }
      }
    }
  }
}
