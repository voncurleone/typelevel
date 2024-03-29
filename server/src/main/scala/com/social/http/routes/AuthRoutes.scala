package com.social.http.routes

import cats.effect.*
import cats.implicits.*
import com.social.http.validation.Syntax.HttpValidationDsl
import com.social.core.Auth
import com.social.domain.auth.{ForgotPasswordInfo, LoginInfo, NewPasswordInfo, RecoverPasswordInfo, NewUserInfo}
import com.social.domain.security.*
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import io.circe.generic.auto.*
import com.social.domain.user.User
import com.social.http.responses.Responses.FailureResponse
import org.http4s.circe.CirceEntityCodec.*
import tsec.authentication.{SecuredRequestHandler, TSecAuthService, asAuthed}

import scala.language.implicitConversions

class AuthRoutes[F[_] : Concurrent: Logger: SecuredHandler] private (auth: Auth[F], authenticator: Authenticator[F])
  extends HttpValidationDsl[F] {

  // POST /auth/login { LoginInfo } => Ok with jwt as authorization: Bearer
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "login" =>
      request.validate[LoginInfo] { loginInfo =>
        val tokenOption = for {
          userOption <- auth.login(loginInfo.email, loginInfo.password)
          //_ <- Logger[F].info(s"User logging in: ${loginInfo.email}")
          //create a new token
          tokenOption <- userOption.traverse(user => authenticator.create(user.email))
        } yield tokenOption

        tokenOption.map {
          case Some(token) => authenticator.embed(Response(Status.Ok), token)
          case None => Response(Status.Unauthorized)
        }
      }
  }

  // POST /auth/users { NewUserInfo } => 201 created or bad request
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "users" =>
      request.validate[NewUserInfo] { userInfo =>
        for {
          userOption <- auth.signUp(userInfo)
          response <- userOption match
            case Some(user) => Status.Created(user)
            case None => Status.BadRequest(FailureResponse(s"User with email ${userInfo.email} already exists"))
        } yield response
      }
  }

  //PUT /auth/users/password { NewPasswordInfo } { authorization: Bearer {jwt} } => 200 ok
  private val changePasswordRoute: AuthRoute[F] = {
    case secureRequest @ PUT -> Root / "users" / "password" asAuthed user =>
      val request = secureRequest.request
      request.validate[NewPasswordInfo] { _ => /*validate doesnt parse payload correctly... _ was $newPasswordInfo*/
        for {
          npi <- request.as[NewPasswordInfo]
          //_ <- Logger[F].info(s"request: $npi")
          //_ <- Logger[F].info(s"changePasswordRoute password indo: $newPasswordInfo")
          userOrError <- auth.changePassword(user.email, npi)
          //_ <- Logger[F].info(s"userOrError: $userOrError")
          response <- userOrError match
            case Right(Some(_)) => Ok()
            case Right(None) => NotFound(FailureResponse(s"User: ${user.email} not found"))
            case Left(_) => Forbidden()

          //_ <- Logger[F].info(s"response: $response")
        } yield response
      }
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

  // DELETE /auth/users/person@domain.com
  private val deleteUserRoute: AuthRoute[F] = {
    case request @ DELETE -> Root / "users" / email asAuthed user =>
      auth.delete(email).flatMap {
        case true => Ok()
        case false => NotFound()
      }
      Ok("fix delete")
  }

  //POST /auth/reset { ForgotPasswordInfo }
  private val forgotPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "reset" =>
      for {
        fpInfo <- request.as[ForgotPasswordInfo]
        //_ <- Logger[F].info(s"forgot pass route2: ${fpInfo.email}")
        _ <- auth.sendPasswordRecoveryToken(fpInfo.email)
        response <- Ok()
      } yield response
  }

  //POST auth/recover { RecoverPasswordInfo }
  private val recoverPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root /"recover" =>
      for {
        rpInfo <- request.as[RecoverPasswordInfo]
        recoverySuccessful <- auth.recoverPasswordFromToken(rpInfo.email, rpInfo.token, rpInfo.newPassword)
        response <- if (recoverySuccessful) Ok() else Forbidden(FailureResponse("Incorrect email/token combo"))
        _ <- Logger[F].info(s"result: $recoverySuccessful")
      } yield response
  }

  private val checkTokenRoute: AuthRoute[F] = {
    case GET -> Root / "checkToken" asAuthed _ =>
      Ok()
  }
  
  val unauthedRoutes = loginRoute <+> createUserRoute <+> forgotPasswordRoute <+> recoverPasswordRoute
  val authedRoutes: HttpRoutes[F] = SecuredHandler[F].liftService(
    changePasswordRoute.restrictedTo(allRoles) |+|
    logoutRoute.restrictedTo(allRoles) |+|
    deleteUserRoute.restrictedTo(adminOnly) |+|
    checkTokenRoute.restrictedTo(allRoles)
    //TSecAuthService(changePasswordRoute.orElse(logoutRoute).orElse(deleteUserRoute))
  )

  val routes: HttpRoutes[F] = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](auth: Auth[F], authenticator: Authenticator[F]) =
    new AuthRoutes[F](auth, authenticator)
}