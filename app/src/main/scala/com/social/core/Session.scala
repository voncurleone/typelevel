package com.social.core

import cats.effect.IO
import com.social.App
import com.social.core.Session.*
import com.social.pages.Page
import tyrian.Cmd
import tyrian.cmds.Logger

final case class Session(email: Option[String] = None, token: Option[String] = None) {
  def update(msg: Msg): (Session, tyrian.Cmd[IO, Msg]) = msg match
    case SetToken(e, t) =>
      (this.copy(email = Some(e), token = Some(t)), Logger.consoleLog[IO](s"setting user session: $e - $t"))

  def initCmd: Cmd[IO, Msg] = Logger.consoleLog("Initializing user session")
}

object Session {
  trait Msg extends App.Msg
  case class SetToken(email: String, token: String) extends Msg
}