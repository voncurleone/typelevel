package com.social.components

import cats.effect.IO
import com.social.pages.Page
import tyrian.{Cmd, Html}

trait Component[Msg, +Model] {
  def initCmd: Cmd[IO, Msg]

  def update(msg: Msg): (Model, Cmd[IO, Msg])

  def view(): Html[Msg]
}
