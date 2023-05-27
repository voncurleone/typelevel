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
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import tsec.common.SecureRandomId
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import concurrent.duration.*

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(userInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, passwordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]
  def authenticator: Authenticator[F]
  //todo: password recovery via email
}

class LiveAuth[F[_]: Async: Logger] private
(users: Users[F], override val authenticator: Authenticator[F]) extends Auth[F] {
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

  override def delete(email: String): F[Boolean] =
    users.delete(email)
}

object LiveAuth {
  def apply[F[_]: Async: Logger](users: Users[F])(securityConfig: SecurityConfig): F[LiveAuth[F]] = {

    //identity store: String => OptionT[F, User]
    val idStore: IdentityStore[F, String, User] = (email: String) =>
      OptionT(users.find(email))

    //backing store for jwt tokensL BackingStore[F, id, JwtToken]
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          OptionT(ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
        override def update(v: JwtToken): F[JwtToken] =
          put(v)
      }
    }


    //key for hashing
    //todo: move secret to config
    val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    //val authenticatorF: F[Authenticator[F]] =
      for {
        key <- keyF
        tokenStore <- tokenStoreF
        //authenticator
        authenticator = JWTAuthenticator.backed.inBearerToken(
          //expiry of token, max idle optional, idStore, key
          expiryDuration = securityConfig.jwtExpiryDuration,
          maxIdle = None,
          identityStore = idStore,
          tokenStore = tokenStore,
          signingKey = key
        )
      } yield new LiveAuth[F](users, authenticator)
  }
}