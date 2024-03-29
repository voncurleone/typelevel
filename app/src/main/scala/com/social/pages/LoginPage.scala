package com.social.pages

import cats.effect.IO
import com.social.App
import com.social.common.*
import com.social.core.Session
import com.social.domain.*
import com.social.domain.auth.LoginInfo
import tyrian.{Cmd, Html}
import tyrian.Html.{`class`, `for`, `type`, button, div, form, h1, id, input, label, name, onClick, onEvent, onInput, span, text}
import tyrian.cmds.Logger
import tyrian.http.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import io.circe.parser.*
import com.social.components.Anchors

final case class LoginPage(
    email: String = "",
    password: String = "",
    status: Option[Page.Status] = None
                          ) extends FormPage("Login", status) {
  import com.social.pages.LoginPage.*

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case UpdateEmail(email) =>
      (this.copy(email = email), Cmd.None)

    case UpdatePassword(password) =>
      (this.copy(password = password), Cmd.None)

    case LoginError(error) =>
      (setErrorStatus(error), Cmd.None)

    case LoginSuccess(token) =>
      (setSuccessStatus("Success!"), Cmd.emit(Session.SetToken(email, token, isFreshUser = true)))

    case AttemptLogin =>
      if !email.matches(Constants.emailRegex) then
        (setErrorStatus("Invalid email!"), Cmd.None)
      else if password.isEmpty then
        (setErrorStatus("Please enter a password"), Cmd.None)
      else (this, Commands.Login(LoginInfo(email, password)))

    case App.NoOp =>
      (this, Cmd.None)

    case a @ _ => (this, Logger.consoleLog(s"Unhandled message: ${a.getClass}"))

  //private stuff
  //ui

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Email", "email", "text", true, UpdateEmail.apply),
    renderInput("Password", "password", "password", true, UpdatePassword.apply),
    button(`type` := "button", onClick(AttemptLogin))("Login"),
    Anchors.renderSimpleNavLink("Forgot password?", Page.Urls.FORGOT_PASSWORD)
  )

  //util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.SUCCESS)))
}

object LoginPage {
  trait Msg extends App.Msg
  case class UpdateEmail(email: String) extends Msg
  case class UpdatePassword(password: String) extends Msg

  //actions
  case object AttemptLogin extends Msg
  case object NoOp extends Msg

  //results
  case class LoginError(error: String) extends Msg
  case class LoginSuccess(token: String) extends Msg

  object Endpoints {
    import com.social.common.Endpoint
    val login: Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.login
      override val method: Method = Method.Post
      override val onResponse: Response => Msg = response => {
        val tokenOption = response.headers.get("authorization")
        tokenOption match
          case Some(token) => LoginSuccess(token)
          case None => LoginError("Invalid username or password")
      }
      override val onError: HttpError => Msg = error => LoginError(error.toString)
    }
  }

  object Commands {
    def Login(loginInfo: LoginInfo): Cmd[IO, Msg] =
      Endpoints.login.call(loginInfo)
  }
}

