package com.social.pages

import cats.effect.IO
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

final case class LoginPage(
    email: String = "",
    password: String = "",
    status: Option[Page.Status] = None
                          ) extends Page {
  import com.social.pages.LoginPage.*

  override def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None

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

    case a @ _ => (this, Logger.consoleLog(s"Unhandled message: $a"))

  override def view(): Html[Page.Msg] =
    div(`class` := "form-section")(
      div(`class` := "top-section")(
        h1("Login")
      ),
      form(name := "Login", `class` := "form", onEvent("submit", e => {
        e.preventDefault()
        NoOp
      }))(
        renderInput("Email", "email", "text", true, UpdateEmail.apply),
        renderInput("Password", "password", "password", true, UpdatePassword.apply),
        button(`type` := "button", onClick(AttemptLogin))("Login"),
        status.map(status => div(status.message)).getOrElse(div())
      )
    )



  //private stuff
  //ui
  private def renderInput(name: String, uid: String, kind: String, isRequired: Boolean, onChange: String => Msg) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if isRequired then span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  //util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.SUCCESS)))
}

object LoginPage {
  trait Msg extends Page.Msg
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
    val login = new Endpoint[Msg] {
      override val location: String = Constants.Endpoints.login
      override val method: Method = Method.Post
      override val onSuccess: Response => Msg = response => {
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

