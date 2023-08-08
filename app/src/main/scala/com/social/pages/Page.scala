package com.social.pages

import cats.effect.IO
import com.social.App
import com.social.components.Component
import com.social.pages.Page.Msg
import tyrian.{Cmd, Html}

abstract class Page extends Component[App.Msg, Page]{
  def initCmd: Cmd[IO, Msg]
  def update(msg: Msg): (Page, Cmd[IO, Msg])
  def view(): Html[Msg]
}

object Page {
  type Msg = App.Msg

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }
  final case class Status(message: String, kind: StatusKind)

  import Urls.*
  def getPage(location: String): Page = location match {
    case `LOGIN` => LoginPage()
    case `SIGN_UP` => SignUpPage()
    case `FORGOT_PASSWORD` => ForgotPasswordPage()
    case `RESET_PASSWORD` => ResetPasswordPage()
    case `PROFILE_PAGE` => ProfilePage()
    case `MAKE_POST` => MakePostPage()
    case `EMPTY` | `HOME` | `POSTS` => PostFeedPage()
    case s"/posts/$id" => PostPage(id)
    case _ => NotFoundPage()
  }

  object Urls {
    val LOGIN = "/login"
    val SIGN_UP = "/signup"
    val FORGOT_PASSWORD = "/forgotpassword"
    val RESET_PASSWORD = "/resetpassword"
    val PROFILE_PAGE = "/profile"
    val MAKE_POST = "/makepost"
    val EMPTY = ""
    val HOME = "/"
    val POSTS = "/posts"
    val Hash = "#"
  }
}