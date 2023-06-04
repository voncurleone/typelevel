package com.social.playground

import cats.effect.*
import org.scalajs.dom.document
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger

import scala.concurrent.duration.*
import scala.scalajs.js.annotation.*

class PlaygroundApp extends TyrianApp[PlaygroundApp.Msg, PlaygroundApp.Model] {
  import PlaygroundApp.*

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(0), Cmd.None)

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.every[IO](1.second).map(_ => Increment(1))

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case Increment(amount) =>
      (model.copy(count = model.count + amount), Logger.consoleLog[IO](s"Incrementing by $amount"))

    case Decrement(amount) =>
      (model.copy(count = model.count - amount), Logger.consoleLog[IO](s"Decrementing by $amount"))

  override def view(model: Model): Html[Msg] =
    div(
      button(onClick(Increment(3)))("increment 3"),
      button(onClick(Decrement(3)))("decrement 3"),
      div(s"Tyrian running: ${model.count}")
    )
}

object PlaygroundApp {
  sealed trait Msg
  case class Increment(amount: Int) extends Msg
  case class Decrement(amount: Int) extends Msg

  case class Model(count: Int)
}
