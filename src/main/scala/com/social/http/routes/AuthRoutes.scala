package com.social.http.routes

import cats.effect.*
import cats.implicits.*
import com.social.http.validation.Syntax.HttpValidationDsl
import com.social.core.Auth
import com.social.domain.auth.{LoginInfo, NewPasswordInfo}
import com.social.domain.security.{AuthRoute, JwtToken}
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import io.circe.generic.auto.*
import com.social.domain.user.{NewUserInfo, User}
import com.social.http.responses.Responses.FailureResponse
import org.http4s.circe.CirceEntityCodec.*
import tsec.authentication.{SecuredRequestHandler, TSecAuthService, asAuthed}

class AuthRoutes[F[_] : Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F] {
  private val authenticator = auth.authenticator
  private val securedHandler: SecuredRequestHandler[F, String, User, JwtToken] = SecuredRequestHandler(authenticator)

  // POST /auth/login { LoginInfo } => Ok with jwt as authorization: Bearer
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "login" =>
      val tokenOption = for {
        loginInfo <- request.as[LoginInfo]
        tokenOption <- auth.login(loginInfo.email, loginInfo.password)
        _ <- Logger[F].info(s"User logging in: ${loginInfo.email}")
      } yield tokenOption

      tokenOption.map {
        case Some(token) => authenticator.embed(Response(Status.Ok), token)
        case None => Response(Status.Unauthorized)
      }
  }

  // POST /auth/users { NewUserInfo } => 201 created or bad request
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "users" =>
      for {
        userInfo <- request.as[NewUserInfo]
        userOption <- auth.signUp(userInfo)
        response <- userOption match
          case Some(user) => Status.Created(user)
          case None => Status.BadRequest(s"User with email ${userInfo.email} already exists")
      } yield response
  }

  //PUT /auth/users/password { NewPasswordInfo } { authorization: Bearer {jwt} } => 200 ok
  private val changePasswordRoute: AuthRoute[F] = {
    case secureRequest @PUT -> Root / "users" / "password" asAuthed user =>
      for {
        newPasswordInfo <- secureRequest.request.as[NewPasswordInfo]
        userOrError <- auth.changePassword(user.email, newPasswordInfo)
        response <- userOrError match
          case Right(Some(_)) => Ok()
          case Right(None) => NotFound(FailureResponse(s"User: ${user.email} not found"))
          case Left(_) => Forbidden()
      } yield response
  }

  // POST /auth/logout { authorization: Bearer {jwt} } => 200 ok
  private val logoutRoute: AuthRoute[F] =  {
    case request @ POST -> Root / "logout" asAuthed _ =>
      val token = request.authenticator
      for {
        _ <- authenticator.discard(token)
        response <- Ok()
      } yield response
  }

  val unauthedRoutes = loginRoute <+> createUserRoute
  val authedRoutes = securedHandler.liftService(
    TSecAuthService(changePasswordRoute.orElse(logoutRoute))
  )

  val routes: HttpRoutes[F] = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) = new AuthRoutes[F](auth)
}