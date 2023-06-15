package com.social.core

import cats.effect.IO
import com.social.App
import com.social.core.Session.*
import com.social.pages.Page
import com.social.common.{Constants, Endpoint}
import tyrian.Cmd
import tyrian.cmds.Logger
import org.scalajs.dom.document
import tyrian.http.{HttpError, Method, Response, Status}

import scala.scalajs.js.Date

final case class Session(email: Option[String] = None, token: Option[String] = None) {
  def update(msg: Msg): (Session, tyrian.Cmd[IO, App.Msg]) = msg match
    case SetToken(e, t, isFreshUser) =>
      val cookieCmd = Commands.setAllSessionCookies(e, t, isFreshUser)
      val routingCmd =
        if(isFreshUser) Cmd.Emit(Router.ChangeLocation(Page.Urls.HOME))
        else Commands.checkToken
      (
        this.copy(email = Some(e), token = Some(t)),
        cookieCmd |+| routingCmd
      )

    //check token action
    case CheckToken =>
      (this, Commands.checkToken)

    case KeepToken =>
      (this, Cmd.None)

    //logout action
    case Logout =>
      val cmd = token.map(_ => Commands.logout).getOrElse(Cmd.None)
      (this, cmd)

    case LogoutSuccess | InvalidateToken =>
      (
        this.copy(email = None, token = None),
        Commands.clearAllSessionCookies() |+| Cmd.Emit(Router.ChangeLocation(Page.Urls.HOME))
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
  //logout action
  case object Logout extends Msg
  case object LogoutSuccess extends Msg
  case object LogoutError extends Msg
  //check token action
  case object CheckToken extends Msg
  case object KeepToken extends Msg
  case object InvalidateToken extends Msg

  def isActive: Boolean =
    getCookie(Constants.cookies.token).nonEmpty

  def getUserToken: Option[String] =
    getCookie(Constants.cookies.token)

  object Endpoints {
    val logout = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.logout
      override val method: Method = Method.Post
      override val onSuccess: Response => Msg = _ => LogoutSuccess
      override val onError: HttpError => Msg = _ => LogoutError
    }

    val checkToken = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.checkToken
      override val method: Method = Method.Get
      override val onSuccess: Response => Msg = response => response.status match
        case Status(200, _) => KeepToken
        case _ => InvalidateToken
      override val onError: HttpError => Msg = _ => InvalidateToken
    }
  }

  object Commands {
    def logout: Cmd[IO, Msg] =
      Endpoints.logout.authorizedCall()

    def setSessionCookie(name: String, value: String, isFresh: Boolean): Cmd[IO, Msg] = {
      Cmd.SideEffect[IO] {
        if getCookie(name).isEmpty || isFresh then
          document.cookie =
            s"$name=$value;expires=${new Date(Date.now() + Constants.cookies.duration)};path=/"
      }
    }

    def setAllSessionCookies(email: String, token: String, isFresh: Boolean = false): Cmd[IO, Msg] = {
      setSessionCookie(Constants.cookies.email, email, isFresh) |+|
        setSessionCookie(Constants.cookies.token, token, isFresh)
    }

    def clearSessionCookie(name: String): Cmd[IO, Msg] = {
      Cmd.SideEffect[IO] {
        document.cookie = s"$name=;expires=${new Date(0)};path=/"
      }
    }

    def clearAllSessionCookies(): Cmd[IO, Msg] = {
      clearSessionCookie(Constants.cookies.email) |+|
        clearSessionCookie(Constants.cookies.token)
    }

    def checkToken: Cmd[IO, Msg] =
      Endpoints.checkToken.authorizedCall()
  }

  private def getCookie(name: String): Option[String] =
    document.cookie
      .split(";")
      .map(_.trim)
      .find(_.startsWith(s"$name="))
      .map(_.split("="))
      .map(_(1))
}