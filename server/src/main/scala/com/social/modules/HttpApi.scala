package com.social.modules

import cats.*
import cats.data.OptionT
import cats.effect.{Async, Ref, Resource}
import cats.effect.kernel.{Concurrent, Sync}
import cats.implicits.*
import com.social.config.SecurityConfig
import com.social.core.Users
import com.social.domain.security.{Authenticator, JwtToken, SecuredHandler}
import com.social.domain.user.User
import com.social.http.routes.{AuthRoutes, HealthRoutes, PostRoutes}
import com.social.modules.Core
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.{BackingStore, IdentityStore, JWTAuthenticator, SecuredRequestHandler}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]) {
  given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes = HealthRoutes[F].routes
  private val postRoutes = PostRoutes[F](core.posts).routes
  private val authRoutes = AuthRoutes[F](core.auth, authenticator).routes

  val endPoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> postRoutes <+> authRoutes)
  )
}

object HttpApi {
  def createAuthenticator[F[_]: Sync](users: Users[F], securityConfig: SecurityConfig): F[Authenticator[F]] = {
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
    } yield authenticator
  }

  def apply[F[_]: Async: Logger ](core: Core[F], securityConfig: SecurityConfig): Resource[F, HttpApi[F]] =
    Resource.eval(createAuthenticator(core.users, securityConfig))
      .map(authenticator => new HttpApi[F](core, authenticator))
    //Resource.pure(new HttpApi[F](core))
}
