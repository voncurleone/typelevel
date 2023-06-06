package com.social.pages
import cats.effect.IO
import tyrian.Html.div
import tyrian.{Cmd, Html}

final case class PostFeedPage() extends Page {
  override def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) =
    (this, Cmd.None)

  override def view(): Html[Page.Msg] =
    div("todo Postfeed")
}
