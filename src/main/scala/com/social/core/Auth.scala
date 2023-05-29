package com.social.core

import cats.data.OptionT
import com.social.domain.auth.*
import com.social.domain.security.*
import com.social.domain.user.*
import tsec.authentication.{AugmentedJWT, BackingStore, IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import cats.effect.*
import cats.implicits.*
import com.social.config.SecurityConfig
import com.social.modules.Emails
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import tsec.common.SecureRandomId
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import concurrent.duration.*

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[User]]
  def signUp(userInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, passwordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]
  def sendPasswordRecoveryToken(email: String): F[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean]
}

class LiveAuth[F[_]: Async: Logger] private
(users: Users[F], tokens: Tokens[F], emails: Emails[F]) extends Auth[F] {
  override def login(email: String, password: String): F[Option[User]] =
    for {
      userOption <- users.find(email)
      validatedUserOption <- userOption.filterA(user =>
        BCrypt.checkpwBool[F](password, PasswordHash[BCrypt](user.hashedPass))
      )
    } yield validatedUserOption

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

  override def delete(email: String): F[Boolean] =
    users.delete(email)

  override def sendPasswordRecoveryToken(email: String): F[Unit] =
    tokens.getToken(email).flatMap {
      case Some(token) => emails.sendPasswordRecoveryEmail(email, token)
      case None => ().pure[F]
    }

  override def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean] =
    for {
      userOption <- users.find(email)
      validToken <- tokens.checkToken(email, token)
      result <- (userOption, validToken) match {
        case (Some(user), true) => updateUser(user, newPassword).map(_.nonEmpty)
        case _ => false.pure[F]
      }
    } yield result
}

object LiveAuth {
  def apply[F[_]: Async: Logger](users: Users[F], tokens: Tokens[F], emails: Emails[F]): F[LiveAuth[F]] = {
    new LiveAuth[F](users, tokens, emails).pure[F]
  }
}