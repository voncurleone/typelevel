package com.social.pages

import cats.effect.IO
import com.social.pages.Page.Msg
import tyrian.{Cmd, Html}

abstract class Page {
  def initCmd: Cmd[IO, Msg]
  def update(msg: Msg): (Page, Cmd[IO, Msg])
  def view(): Html[Msg]
}

object Page {
  trait Msg

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }
  final case class Status(message: String, kind: StatusKind)

  import Urls.*
  def getPage(location: String) = location match {
    case `LOGIN` => LoginPage()
    case `SIGN_UP` => SignUpPage()
    case `FORGOT_PASSWORD` => ForgotPasswordPage()
    case `RECOVER_PASSWORD` => RecoverPasswordPage()
    case `EMPTY` | `HOME` | `POSTS` => PostFeedPage()
    case s"/posts/$id" => PostPage(id)
    case _ => NotFoundPage()
  }

  object Urls {
    val LOGIN = "/login"
    val SIGN_UP = "/signup"
    val FORGOT_PASSWORD = "/forgotpassword"
    val RECOVER_PASSWORD = "/recoverpassword"
    val EMPTY = ""
    val HOME = "/"
    val POSTS = "/posts"
  }
}