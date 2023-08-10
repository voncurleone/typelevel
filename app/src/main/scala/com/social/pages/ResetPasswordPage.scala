package com.social.pages

import cats.effect.IO
import com.social.common.*
import com.social.App
import com.social.domain.auth.RecoverPasswordInfo
import com.social.pages.ForgotPasswordPage.AttemptResetPassword
import com.social.pages.Page
import com.social.pages.ResetPasswordPage.*
import tyrian.{Cmd, Html}
import tyrian.Html.*
import tyrian.http.{HttpError, Method, Response, Status}
import io.circe.parser.*
import io.circe.generic.auto.*
import com.social.components.Anchors


final case class ResetPasswordPage(email: String = "",
                                     password: String = "",
                                     token: String = "",
                                     status: Option[Page.Status] = None)
  extends FormPage("Reset password", status) {

    override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
      case UpdateEmail(email) =>
        (this.copy(email = email), Cmd.None)

      case UpdateToken(token) =>
        (this.copy(token = token), Cmd.None)

      case UpdatePassword(password) =>
        (this.copy(password = password), Cmd.None)

      //action
      case AttemptResetPassword =>
        if (!email.matches(Constants.emailRegex))
          (setErrorStatus("Enter a valid email."), Cmd.None)
        else if(token.isEmpty)
          (setErrorStatus("Please enter a token."), Cmd.None)
        else if(password.isEmpty)
          (setErrorStatus("Please enter a password."), Cmd.None)
        else
          (this, Commands.resetPassword(email, token, password))

      case ResetPasswordFailure(error) =>
        (setErrorStatus(error), Cmd.None)

      case ResetPasswordSuccess =>
        (setSuccessStatus("Your password has been reset."), Cmd.None)

      case _ => (this, Cmd.None)

    override protected def renderFormContent(): List[Html[App.Msg]] = List(
      renderInput("Email", "email", "text", true, UpdateEmail.apply),
      renderInput("Token", "token", "text", true, UpdateToken.apply),
      renderInput("New Password", "password", "password", true, UpdatePassword.apply),
      button(`type` := "button", onClick(AttemptResetPassword))("Reset"),
      Anchors.renderSimpleNavLink("Get a token", Page.Urls.FORGOT_PASSWORD)
    )

    //util
    def setErrorStatus(message: String): Page =
      this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.ERROR)))

    def setSuccessStatus(message: String): Page =
      this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.SUCCESS)))
}

object ResetPasswordPage {
  trait Msg extends App.Msg
  case class UpdateEmail(email: String) extends Msg
  case class UpdateToken(token: String) extends Msg
  case class UpdatePassword(password: String) extends Msg
  case class ResetPasswordFailure(error: String) extends Msg
  case object AttemptResetPassword extends Msg
  case object ResetPasswordSuccess extends Msg

  object Endpoints {
    val resetPassword: Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.resetPassword
      override val method: Method = Method.Post
      override val onError: HttpError => Msg = e => ResetPasswordFailure(e.toString)
      override val onResponse: Response => Msg = response => response.status match
        case Status(200, _) => ResetPasswordSuccess
        case Status(s, _ ) if s >= 400 && s < 500 =>
          val json = response.body
          val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
          parsed match {
            case Left(error) => ResetPasswordFailure(s"Response error: ${error.getMessage}")
            case Right(serverError) => ResetPasswordFailure(serverError)
          }

    }
  }

  object Commands {
    def resetPassword(email: String, token: String, password: String): Cmd[IO, Msg] =
      Endpoints.resetPassword.call(RecoverPasswordInfo(email, token, password))
  }
}
