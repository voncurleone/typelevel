package com.social.pages
import cats.effect.IO
import com.social.App
import tyrian.{Cmd, Html}
import ProfilePage.*
import com.social.common.*
import com.social.core.Session
import com.social.domain.auth.NewPasswordInfo
import tyrian.Html.*
import tyrian.http.{HttpError, Method, Response}
import io.circe.generic.auto.*
import tyrian.cmds.Logger
import tyrian.http.*

final case class ProfilePage(currentPass: String = "",
                             newPass: String = "",
                             newPass2: String = "",
                             status: Option[Page.Status] = None)
  extends FormPage("Change password", status) {

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Current password", "current-password", "password", true, UpdatePassword.apply),
    renderInput("New password", "new-password", "password", true, UpdateNewPass.apply),
    renderInput("Confirm new password", "validate-new-password", "password", true, UpdateNewPassConfirm.apply),
    button(`type` := "button", onClick(AttemptPasswordReset))("Reset Password")
  )

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case UpdatePassword(password) =>
      (this.copy(currentPass = password), Cmd.None)

    case UpdateNewPass(password) =>
      (this.copy(newPass = password), Cmd.None)

    case UpdateNewPassConfirm(password) =>
      (this.copy(newPass2 = password), Cmd.None)

    case ResetFailure(error) =>
      (setErrorStatus(error), Cmd.None)

    case ResetSuccess =>
      (setSuccessStatus("Password has been reset."), Cmd.None)

    case AttemptPasswordReset =>
      if( newPass.isEmpty )
        (setErrorStatus("Password must not be empty."), Cmd.None)
      else if( newPass != newPass2 )
        (setErrorStatus("New passwords do not match."), Cmd.None)
      else
        (this, Commands.resetPassword(currentPass, newPass)
          /*|+| Logger.consoleLog(s"currentPass: $currentPass, newPass: $newPass")*/)

    case _ => (this, Cmd.None)

  override def view(): Html[Page.Msg] =
    if(Session.isActive)
      super.view()
    else
      renderInvalidPage

  private def renderInvalidPage =
    div(
      h1("Profile"),
      div("You're not logged in.")
    )
  //util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.SUCCESS)))
}

object ProfilePage {
  trait Msg extends App.Msg
  case class UpdatePassword(password: String) extends Msg
  case class UpdateNewPass(password: String) extends Msg
  case class UpdateNewPassConfirm(password: String) extends Msg
  case class ResetFailure(error: String) extends Msg
  case object AttemptPasswordReset extends Msg
  case object ResetSuccess extends Msg

  object Endpoints {
    val resetPassword: Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.profileReset
      override val method: Method = Method.Put
      override val onError: HttpError => Msg = e => ResetFailure(e.toString)
      override val onResponse: Response => Msg = _.status match
        case Status(200, _) => ResetSuccess
        case Status(404, _) => ResetFailure("User not found.")
        case Status(s, _) if s >= 400 && s < 500 => ResetFailure("Invalid credentials.")
        case _ => ResetFailure("Unknown reply from server.")
    }
  }

  object Commands {
    def resetPassword(oldPass: String, newPass: String): Cmd[IO, Msg] =
      val npi = NewPasswordInfo(oldPass, newPass)
      Endpoints.resetPassword.authorizedCall(npi)
        //|+| Logger.consoleLog(s"oldPass: $oldPass, newPass: $newPass, npi: $npi")
  }
}
