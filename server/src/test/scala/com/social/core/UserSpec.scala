package com.social.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.social.fixtures.UserFixture
import doobie.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.social.domain.user.*
import org.postgresql.util.PSQLException
import org.scalatest.Inside

class UserSpec
  extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Inside
    with DoobieSpec
    with UserFixture {
  override val initScript: String = "sql/users.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "User algebra" - {
    "should return user based on email" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          user <- users.find("person@domain.com")
          //retrieved <- users.find()
        } yield (user)

        program.asserting(_ shouldBe Some(person))
      }
    }

    "should return none if user does not exist " in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          user <- users.find("notperson@domain.com")
          //retrieved <- users.find()
        } yield (user)

        program.asserting(_ shouldBe None)
      }
    }

    "should create a new user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          email <- users.create(newUser)
          userOption <- sql"SELECT * FROM users WHERE email = ${newUser.email}"
                          .query[User]
                          .option
                          .transact(xa)
        } yield (email, userOption)

        program.asserting {
          case (email, userOption) =>
            email shouldBe Some(newUser.email)
            userOption shouldBe Some(newUser)
        }
      }
    }

    "should fail creating a new user if the email exists" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          email <- users.create(person).attempt
          //retrieved <- users.find()
        } yield (email)

        program.asserting { outcome =>
          inside(outcome) {
            case Left(value) => value shouldBe a[PSQLException]
            case _ => fail()
          }
        }
      }
    }

    "should return none when updating a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          result <- users.update(newUser)
        } yield result

        program.asserting(_ shouldBe None)
      }
    }

    "should update an existing user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          result <- users.update(person.copy(hashedPass = updatedPerson.hashedPass))
        } yield result

        program.asserting(_ shouldBe Some(updatedPerson))
      }
    }

    "should delete a user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          _ <- IO(println("TEST"))
          result <- users.delete(person.email)//this line?
          userOption <- sql"SELECT * FROM users WHERE email = ${person.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (result, userOption)

        program.asserting {
          case (result, userOption) =>
            result shouldBe true
            userOption shouldBe None
        }
      }
    }

    "should not delete a user that doesnt exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          result <- users.delete("notperson@gmail.com")
        } yield result

        program.asserting(_ shouldBe false)
      }
    }
  }
}
