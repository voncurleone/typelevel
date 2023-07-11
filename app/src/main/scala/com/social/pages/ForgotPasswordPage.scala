package com.social.pages

import cats.effect.IO
import com.social.App
import tyrian.{Cmd, Html}
import tyrian.Html.*
import com.social.common.{Constants, Endpoint}
import com.social.domain.auth.ForgotPasswordInfo
import tyrian.http.{HttpError, Method, Response}
import io.circe.generic.auto.*

final case class ForgotPasswordPage(email: String = "", status: Option[Page.Status] = None)
  extends FormPage("Reset password", status) {
    import com.social.pages.ForgotPasswordPage.*

    override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match {
      case UpdateEmail(email) =>
        (this.copy(email = email), Cmd.None)

      case AttemptResetPassword =>
        if(!email.matches(Constants.emailRegex))
          (setErrorStatus("Enter a valid email."), Cmd.None)
        else
          (this, Commands.resetPassword(email))

      case ResetFailure(error) =>
        (setErrorStatus(error), Cmd.None)

      case ResetSuccess =>
        (setSuccessStatus("Check your email!"), Cmd.None)

      case _ => (this, Cmd.None)
    }


    override protected def renderFormContent(): List[Html[App.Msg]] = List(
      renderInput("Email", "email", "text", true, UpdateEmail.apply),
      button(`type` := "button", onClick(AttemptResetPassword))("Send email")
    )

  //util
    def setErrorStatus(message: String): Page =
      this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.ERROR)))

    def setSuccessStatus(message: String): Page =
      this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.SUCCESS)))
}

object ForgotPasswordPage {
  trait Msg extends App.Msg
  case class UpdateEmail(email: String) extends Msg
  case object AttemptResetPassword extends Msg
  case class ResetFailure(error: String) extends Msg
  case object ResetSuccess extends Msg

  object Endpoints {
    val resetPassword: Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.forgotPassword
      override val method: Method = Method.Post
      override val onError: HttpError => Msg = e => ResetFailure(e.toString)
      override val onResponse: Response => Msg = _ => ResetSuccess
    }
  }

  object Commands {
    def resetPassword(email: String): Cmd[IO, Msg] =
      Endpoints.resetPassword.call(ForgotPasswordInfo(email))
  }
}