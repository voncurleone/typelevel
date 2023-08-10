package com.social.components

import com.social.App
import com.social.core.{Router, Session}
import com.social.pages.Page.Urls.*
import tyrian.{Cmd, Html}
import tyrian.Html.*
import com.social.components.Anchors

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
      Anchors.renderSimpleNavLink("Posts", POSTS),
      Anchors.renderSimpleNavLink("Make a post", MAKE_POST)
    )

    val unauthedLinks = List(
      Anchors.renderSimpleNavLink("Login", LOGIN),
      Anchors.renderSimpleNavLink("Sign Up", SIGN_UP)
    )

    val authedLinks = List(
      Anchors.renderSimpleNavLink("Profile", PROFILE_PAGE),
      Anchors.renderNavLink("Log out", Hash)(_ => Session.Logout)
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
}
