package com.social.core

import com.social.domain.auth.*
import com.social.domain.security.*
import com.social.domain.user.*

import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import cats.effect.*
import cats.implicits.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(userInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, passwordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
}

class LiveAuth[F[_]: MonadCancelThrow: Logger] private(users: Users[F], authenticator: Authenticator[F]) extends Auth[F] {
  override def login(email: String, password: String): F[Option[JwtToken]] = ???
  override def signUp(userInfo: NewUserInfo): F[Option[User]] = ???
  override def changePassword(email: String, passwordInfo: NewPasswordInfo): F[Either[String, Option[User]]] = ???
}

object LiveAuth {
  def apply[F[_]: MonadCancelThrow: Logger](users: Users[F], authenticator: Authenticator[F]): F[LiveAuth[F]] =
    new LiveAuth[F](users, authenticator).pure[F]
}