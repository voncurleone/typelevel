package com.social.core

import cats.effect.*
import cats.implicits.*
import cats.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import org.typelevel.log4cats.Logger

import com.social.domain.user.*
import com.social.logging.Syntax.*

trait Users[F[_]] {
  def find(email: String): F[Option[User]]
  def create(user: User): F[Option[String]]
  def update(user: User): F[Option[User]]
  def delete(email: String): F[Boolean]
}

final class LiveUsers[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]) extends Users[F] {
  override def find(email: String): F[Option[User]] =
    sql"SELECT * FROM users WHERE email = $email"
      .query[User]
      .option
      .transact(xa)

  override def create(user: User): F[Option[String]] =
    sql"""
         INSERT INTO users(
                            email,
                            handle,
                            hashedPass,
                            firstName,
                            lastName,
                            role
         ) VALUES (
                    ${user.email},
                    ${user.handle},
                    ${user.hashedPass},
                    ${user.firstName},
                    ${user.lastName},
                    ${user.role}
         )
     """
      .update
      .run
      .transact(xa)
      .map(_ => Some(user.email))

  override def update(user: User): F[Option[User]] =
    for {
      _ <-
        sql"""
          UPDATE users SET
            email = ${user.email},
            handle = ${user.handle},
            hashedPass = ${user.hashedPass},
            firstName = ${user.firstName},
            lastName = ${user.lastName},
            role = ${user.role}
          WHERE email = ${user.email}
       """
          .update
          .run
          .transact(xa)

      userOption <- find(user.email)
    } yield userOption

  override def delete(email: String): F[Boolean] =
    sql"DELETE FROM users WHERE email=${email}"
      .update
      .run
      .transact(xa)
      .map(_ > 0)
}

object LiveUsers {
  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveUsers[F]] = new LiveUsers[F](xa).pure[F]
}