package com.social.pages

import cats.effect.IO
import com.social.App
import com.social.core.Router
import com.social.pages.Page.Msg
import org.scalajs.dom.HTMLFormElement
import tyrian.{Cmd, Html}
import tyrian.Html.*
import org.scalajs.dom.*
import scala.concurrent.duration.DurationInt

abstract class FormPage(title: String, status: Option[Page.Status]) extends Page{
  //abstract api
  protected def renderFormContent(): List[Html[App.Msg]] // for every page to override

  //public api
  override def initCmd: Cmd[IO, Msg] = clearForm()

  override def view(): Html[Msg] =
    renderForm()

  //protected api
  protected def renderForm(): Html[App.Msg] =
    div(`class` := "form-section")(
      div(`class` := "top-section")(
        h1(title)
      ),
      form(name := "Login", id := "form", `class` := "form", onEvent("submit", e => {
        e.preventDefault()
        App.NoOp
      }))(
        renderFormContent()
      ),
      status.map(status => div(status.message)).getOrElse(div())
    )

  protected def renderInput(name: String, uid: String, kind: String, isRequired: Boolean, onChange: String => Msg) =
    div(`class` := "form-input")(
      label(`for` := uid, `class` := "form-label")(
        if isRequired then span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  protected def renderAuxLink(location: String, text: String): Html[App.Msg] =
    a(href := location, `class` := "aux-link", onEvent("click", e => {
      e.preventDefault() //prevent page from reloading
      Router.ChangeLocation(location)
    }))(text)

  //private
  private def clearForm() =
    Cmd.Run[IO, Unit, App.Msg] {
      def effect: IO[Option[HTMLFormElement]] = for {
        maybeForm <- IO(Option(document.getElementById("form").asInstanceOf[HTMLFormElement]))
        finalForm <-
          if(maybeForm.isEmpty) IO.sleep(100.millis) *> effect
          else IO(maybeForm)
      } yield finalForm

      effect.map(_.foreach(_.reset()))
    }(_ => App.NoOp)
}
