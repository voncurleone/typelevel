package com.social

import cats.effect.*
import com.social.App.Msg
import com.social.components.Header
import com.social.core.{Router, Session}
import com.social.pages.Page

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
    val location = window.location.pathname
    val page = Page.getPage(location)
    val pageCmd = page.initCmd
    val (router, routerCmd) = Router.startAt(location)
    val session = Session()
    val sessionCmd = session.initCmd
    (Model(router, session, page), routerCmd |+| pageCmd |+| sessionCmd)

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.make("urlChange", model.router.history.state.discrete //listener for browser history changes
      .map(_.get)
      .map( newLocation => Router.ChangeLocation(newLocation, true))
    )

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case msg: Router.Msg =>
      val (newRouter, routerCmd) = model.router.update(msg)
      if model.router == newRouter then
        (model, Cmd.None)
      else
        val newPage = Page.getPage(newRouter.location)
        val newPageCmd = newPage.initCmd
        (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)

    case msg: Session.Msg =>
      val (newSession, sessionCmd) = model.session.update(msg)
      (model.copy(session = newSession), sessionCmd)

    case msg: App.Msg =>
      val (newPage, cmd) = model.page.update(msg)
      (model.copy(page = newPage), cmd)


  override def view(model: Model): Html[Msg] =
    div(
      Header.view(),
      model.page.view()
    )
}

object App {
  //type Msg = Router.Msg | Page.Msg
  trait Msg
  case object NoOp extends Msg
  
  case class Model(router: Router, session: Session, page: Page)
}
