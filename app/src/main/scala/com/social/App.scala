package com.social

import cats.effect.*
import com.social.App.Msg
import com.social.core.Router

import scala.scalajs.js.annotation.*
import org.scalajs.dom.{document, window}
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger

import concurrent.duration.*

@JSExportTopLevel("App")
class App extends TyrianApp[App.Msg, App.Model] {
  import com.social.App.*

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    val (router, cmd) = Router.startAt(window.location.pathname)
    (Model(router), cmd)

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.make("urlChange", model.router.history.state.discrete //listener for browser history changes
      .map(_.get)
      .map( newLocation => Router.ChangeLocation(newLocation, true))
    )

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case msg: Msg =>
      val (newRouter, cmd) = model.router.update(msg)
      (model.copy(router = newRouter), cmd)

  override def view(model: Model): Html[Msg] =
    div(
      renderNavLink("Posts", "/posts"),
      renderNavLink("Login", "/login"),
      renderNavLink("Sign Up", "/signup"),
      div(s"You are now at: ${model.router.location}")
    )

  private def renderNavLink(text: String, location: String) =
    a(href := location, `class` := "nav-link", onEvent("click", e => {
      e.preventDefault() //prevent page from reloading
      Router.ChangeLocation(location)
    }))(text)
}

object App {
  type Msg = Router.Msg

  case class Model(router: Router)
}
