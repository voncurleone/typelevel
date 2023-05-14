package com.social.http.routes

import cats.effect.*
import cats.implicits.*
import com.social.http.validation.Syntax.HttpValidationDsl
import com.social.core.Auth
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

class AuthRoutes[F[_] : Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F] {
  // POST /auth/login { LoginInfo } => Ok with jwt as authorization: Bearer
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "login" =>
      Ok("login stub")
  }

  // POST /auth/users { NewUserInfo } => 201 created or bad request
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "users" =>
      Ok("create user stub")
  }

  //PUT /auth/users/password { NewPasswordInfo } { authorization: Bearer {jwt} } => 200 ok
  private val changePasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case PUT -> Root / "users" / "password" =>
      Ok("change password stub")
  }

  // POST /auth/logout { authorization: Bearer {jwt} } => 200 ok
  private val logoutRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "logout" =>
      Ok("logout stub")
  }

  val routes: HttpRoutes[F] = Router(
    "/auth" -> (loginRoute <+> createUserRoute <+> changePasswordRoute <+> logoutRoute)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) = new AuthRoutes[F](auth)
}