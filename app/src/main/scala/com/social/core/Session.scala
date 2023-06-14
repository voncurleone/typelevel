package com.social.core

import cats.effect.IO
import com.social.App
import com.social.core.Session.*
import com.social.pages.Page
import com.social.common.Constants
import tyrian.Cmd
import tyrian.cmds.Logger
import org.scalajs.dom.document

import scala.scalajs.js.Date

final case class Session(email: Option[String] = None, token: Option[String] = None) {
  def update(msg: Msg): (Session, tyrian.Cmd[IO, Msg]) = msg match
    case SetToken(e, t, isFreshUser) =>
      (
        this.copy(email = Some(e), token = Some(t)),
        Commands.setAllSessionCookies(e, t, isFreshUser)
      )

  def initCmd: Cmd[IO, Msg] =
    val commandOption = for {
      email <- getCookie(Constants.cookies.email)
      token <- getCookie(Constants.cookies.token)
    } yield Cmd.Emit(SetToken(email, token, isFreshUser = false))

    commandOption.getOrElse(Cmd.None)
}

object Session {
  trait Msg extends App.Msg
  case class SetToken(email: String, token: String, isFreshUser: Boolean = false) extends Msg

  object Commands {
    def setSessionCookie(name: String, value: String, isFresh: Boolean): Cmd[IO, Msg] = {
      Cmd.SideEffect[IO] {
        if getCookie(name).isEmpty || isFresh then
          document.cookie =
            s"$name=$value; expires=${new Date(Date.now() + Constants.cookies.duration)}; path=/"
      }
    }

    def setAllSessionCookies(email: String, token: String, isFresh: Boolean = false): Cmd[IO, Msg] = {
      setSessionCookie(Constants.cookies.email, email, isFresh) |+|
        setSessionCookie(Constants.cookies.token, token, isFresh)
    }

    def clearSessionCookie(name: String): Cmd[IO, Msg] = {
      Cmd.SideEffect[IO] {
        document.cookie = s"$name=; expires=${new Date(0)}; path=/"
      }
    }

    def clearAllSessionCookies(): Cmd[IO, Msg] = {
      clearSessionCookie(Constants.cookies.email) |+|
        clearSessionCookie(Constants.cookies.token)
    }
  }

  private def getCookie(name: String): Option[String] =
    document.cookie
      .split(";")
      .map(_.trim)
      .find(_.startsWith(s"$name="))
      .map(_.split("="))
      .map(_(1))
}