package com.social.domain

import cats.*
import cats.implicits.*
import cats.syntax.*
import tsec.authentication.{AugmentedJWT, JWTAuthenticator, SecuredRequest, SecuredRequestHandler, TSecAuthService}
import tsec.mac.jca.HMACSHA256
import com.social.domain.user.*
import org.http4s.{Response, Status}
import tsec.authorization.{AuthorizationInfo, BasicRBAC}

object security {
  type Crypto = HMACSHA256
  type JwtToken = AugmentedJWT[Crypto, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  
  //type aliases for http routes
  type AuthRoute[F[_]] = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  type SecuredHandler[F[_]] = SecuredRequestHandler[F, String, User, JwtToken]
  object SecuredHandler {
    def apply[F[_]](using handler: SecuredHandler[F]): SecuredHandler[F] = handler
  }

  //RBAC - role based access control
  type AuthRBAC[F[_]] = BasicRBAC[F, Role, User, JwtToken]
  
  // BasicRBAC[F, Role, User, JwtToken
  given authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(user: User): F[Role] = user.role.pure[F]
  }

  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC.all[F, Role, User, JwtToken]

  def registeredOnly[F[_] : MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.ADMIN, Role.USER)

  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.ADMIN)

  //authorizations
  case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])
  
  object Authorizations {
    given combiner[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance { (auth1, auth2) =>
      Authorizations(auth1.rbacRoutes |+| auth2.rbacRoutes)
    }
  }

  // authRoute -> Authorization -> TsecAuthService -> httpRoute

  // authRoute -> Authorization = .restrictedTo extension method
  extension [F[_]] (authRoute: AuthRoute[F])
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))

  // Authorization -> TSecAuthService = implicit conversion
  given auth2tsec[F[_]: Monad]: Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] =
  auths => {
    //always responds with 401
    val unauthorizedService: TSecAuthService[User, JwtToken, F]  =
      TSecAuthService[User, JwtToken, F] { _ =>
      Response[F](Status.Unauthorized).pure[F]
    }

    auths.rbacRoutes
      .toSeq
      .foldLeft(unauthorizedService) {
        case (acc, (rbac, routes)) =>
          //merge routes into one
          val bigRoute = routes.reduce(_.orElse(_))
          //build a new service, fall back to the acc if rbac/route fails
          TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
      }
  }
  
  //semigroup for authorizatioj
}
