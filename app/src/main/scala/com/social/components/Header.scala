package com.social.components

import com.social.core.Router
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
          renderNavLink("Posts", "/posts"),
          renderNavLink("Login", "/login"),
          renderNavLink("Sign Up", "/signup")
        )
      )
    )

  //private api
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

  private def renderNavLink(text: String, location: String) =
    li(`class` := "nav-item")(
      a(href := location, `class` := "nav-link", onEvent("click", e => {
        e.preventDefault() //prevent page from reloading
        Router.ChangeLocation(location)
      }))(text)
    )
}
