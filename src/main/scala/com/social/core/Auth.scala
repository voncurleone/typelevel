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
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(userInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, passwordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
  //todo: password recovery via email
}

class LiveAuth[F[_]: Async: Logger] private(users: Users[F], authenticator: Authenticator[F]) extends Auth[F] {
  override def login(email: String, password: String): F[Option[JwtToken]] =
    for {
      userOption <- users.find(email)
      validatedUserOption <- userOption.filterA(user =>
        BCrypt.checkpwBool[F](password, PasswordHash[BCrypt](user.hashedPass))
      )

      tokenOption <- validatedUserOption.traverse(user => authenticator.create(user.email))
    } yield tokenOption

  override def signUp(userInfo: NewUserInfo): F[Option[User]] =
    users.find(userInfo.email).flatMap {
      case Some(_) => None.pure[F]
      case None => for {
        hashedPass <- BCrypt.hashpw[F](userInfo.pass)
        user <- User(
          userInfo.email,
          userInfo.handle,
          hashedPass,
          userInfo.firstName,
          userInfo.lastName,
          Role.USER
        ).pure[F]

        _ <- users.create(user)
      } yield Some(user)
    }

  override def changePassword(email: String, passwordInfo: NewPasswordInfo): F[Either[String, Option[User]]] =
    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) => checkAndUpdate(user, passwordInfo)
    }

  private def updateUser(user: User, newPass: String): F[Option[User]] = for {
    hashedPass <- BCrypt.hashpw[F](newPass)
    updatedUser <- users.update(user.copy(hashedPass = hashedPass))
  } yield updatedUser

  private def checkAndUpdate(user: User, passwordInfo: NewPasswordInfo): F[Either[String, Option[User]]] = for {
    isValidPass <- BCrypt.checkpwBool[F](passwordInfo.oldPassword, PasswordHash[BCrypt](user.hashedPass))
    updateResult <-
      if isValidPass then updateUser(user, passwordInfo.newPassword).map(Right(_))
      else Left("Invalid password").pure[F]
  } yield updateResult
}

object LiveAuth {
  def apply[F[_]: Async: Logger](users: Users[F], authenticator: Authenticator[F]): F[LiveAuth[F]] =
    new LiveAuth[F](users, authenticator).pure[F]
}