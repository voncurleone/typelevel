package com.social.pages

import cats.effect.IO
import tyrian.{Cmd, Html}
import tyrian.Html.*
import tyrian.cmds.Logger
import com.social.common.Constants.emailRegex

final case class SignUpPage(
   email: String = "",
   handle: String = "",
   pass: String = "",
   confirmPassword: String = "",
   firstName: String = "",
   lastName: String = "",
   status: Option[Page.Status] = None
                           ) extends Page {
  import com.social.pages.SignUpPage.*

  override def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match {
    case UpdateEmail(email) =>
      (this.copy(email = email), Cmd.None /*Logger.consoleLog(s"Changing email to: $email")*/ )

    case UpdateHandle(handle) =>
      (this.copy(handle = handle), Cmd.None)

    case UpdatePassword(password) =>
      (this.copy(pass = password), Cmd.None)

    case UpdateConfirmPassword(confirmPassword) =>
      (this.copy(confirmPassword = confirmPassword), Cmd.None)

    case UpdateFirstName(firstName) =>
      (this.copy(firstName = firstName), Cmd.None)

    case UpdateLastName(lastName) =>
      (this.copy(lastName = lastName), Cmd.None)

    case AttemptSignUp =>
      if (!email.matches(emailRegex))
        (setErrorStatus("Email is invalid!"), Cmd.None)
      else if (pass.isEmpty)
        (setErrorStatus("Please enter a password."), Cmd.None)
      else if (pass != confirmPassword)
        (setErrorStatus("Passwords do not match."), Cmd.None)
      else (this, Logger.consoleLog[IO]("Signing up!", email, pass, handle, firstName, lastName))

    case _ =>
      (this, Cmd.None)
  }

  override def view(): Html[Page.Msg] =
    div(`class` := "form-section")(
      div(`class` := "top-section")(
        h1("Sign Up")
      ),
      form(name := "signin", `class` := "form", onEvent("submit", e => {
        e.preventDefault()
        NoOp
      }))(
        renderInput("Email", "email", "text", true, UpdateEmail.apply),
        renderInput("Handle", "handle", "text", true, UpdateHandle.apply),
        renderInput("Password", "password", "password", true, UpdatePassword.apply),
        renderInput("Confirm Password", "confirmPassword", "password", true, UpdateConfirmPassword.apply),
        renderInput("First Name", "firstName", "text", false, UpdateFirstName.apply),
        renderInput("Last Name", "lastName", "text", false, UpdateLastName.apply),
        button(`type` := "button", onClick(AttemptSignUp))("Sign Up"),
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
}

object SignUpPage {
  trait Msg extends Page.Msg
  case class UpdateEmail(email: String) extends Msg
  case class UpdateHandle(handle: String) extends Msg
  case class UpdatePassword(password: String) extends Msg
  case class UpdateConfirmPassword(confirmPassword: String) extends Msg
  case class UpdateFirstName(firstName: String) extends Msg
  case class UpdateLastName(lastName: String) extends Msg

  //actions
  case object AttemptSignUp extends Msg
  case object NoOp extends Msg
}
