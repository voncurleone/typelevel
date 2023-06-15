package com.social.components

import com.social.App
import com.social.core.{Router, Session}
import com.social.pages.Page.Urls.*
import tyrian.{Cmd, Html}
import tyrian.Html.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Header {
  //public api
  def view() =
    div(`class` := "header-container")(
      renderLogo(),
      div(`class` := "header-nav")(
        ul(`class` := "header-links") (
          renderNavLinks()
        )
      )
    )

  //private api
  private def renderNavLinks(): List[Html[App.Msg]] = {
    val constantLinks = List(
      renderSimpleNavLink("Posts", POSTS)
    )

    val unauthedLinks = List(
      renderSimpleNavLink("Login", LOGIN),
      renderSimpleNavLink("Sign Up", SIGN_UP)
    )

    val authedLinks = List(
      renderNavLink("Log out", Hash)(_ => Session.Logout)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
  }

  @js.native
  @JSImport("/static/img/logo2.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(href := "/", onEvent("click", e => {
      e.preventDefault() //prevent page from reloading
      Router.ChangeLocation("/")
    }))(
      img(
        `class` := "home-logo",
        src := logoImage,
        alt := "logo image"
      )
    )

  private def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  private def renderNavLink(text: String, location: String)(location2Msg: String => App.Msg) =
    li(`class` := "nav-item")(
      a(href := location, `class` := "nav-link", onEvent("click", e => {
        e.preventDefault() //prevent page from reloading
        location2Msg(location)
      }))(text)
    )
}
