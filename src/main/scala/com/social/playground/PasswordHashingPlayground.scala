package com.social.playground

import cats.effect.{IO, IOApp}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

object PasswordHashingPlayground extends IOApp.Simple {
  override def run: IO[Unit] =
    BCrypt.hashpw[IO]("pass").flatMap(IO.println) *>
    BCrypt.checkpwBool[IO]("newpassword", PasswordHash[BCrypt]("$2a$10$XsVNGFXgMKSdXv18LLA2LuksMMwqFiHWxQzIkqYok96SXDVwNadva"))
      .flatMap(IO.println)
}
