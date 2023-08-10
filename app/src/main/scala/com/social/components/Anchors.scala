package com.social.components

import com.social.App
import com.social.core.Router
import tyrian.Html.{`class`, a, href, li, onEvent}

object Anchors {
  def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  def renderNavLink(text: String, location: String)(location2Msg: String => App.Msg) =
    li(`class` := "nav-item")(
      a(href := location, `class` := "nav-link", onEvent("click", e => {
        e.preventDefault() //prevent page from reloading
        location2Msg(location)
      }))(text)
    )
}
