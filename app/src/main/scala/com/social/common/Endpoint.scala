package com.social.common

import cats.effect.IO
import com.social.core.Session
import io.circe.Encoder
import io.circe.syntax.*
import tyrian.Cmd
import tyrian.http.{Body, Decoder, Header, Http, HttpError, Method, Request, Response, Status}
import io.circe.parser.*

trait Endpoint[M] {
  val location: String
  val method: Method
  val onResponse: Response => M
  val onError: HttpError => M

  //public api
  def call[A: Encoder](payload: A): Cmd[IO, M] =
    internalCall(payload, None)

  def call(): Cmd[IO, M] =
    internalCall(None)

  def authorizedCall[A: Encoder](payload: A): Cmd[IO, M] =
    internalCall(payload, Session.getUserToken)

  def authorizedCall(): Cmd[IO, M] =
    internalCall(Session.getUserToken)

  //private api
  private def internalCall[A: Encoder](payload: A, authorization: Option[String]): Cmd[IO, M] =
    Http.send(
      Request(
        method,
        headers = authorization.map(token => Header("Authorization", token)).toList,
        url = location,
        body = Body.json(payload.asJson.toString),
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )

  private def internalCall(authorization: Option[String]): Cmd[IO, M] =
    Http.send(
      Request(
        method,
        headers = authorization.map(token => Header("Authorization", token)).toList,
        url = location,
        body = Body.Empty,
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )
}

object Endpoint {
  def onResponse[A: io.circe.Decoder, Msg](valueCB: A => Msg, errorCB: String => Msg): Response => Msg =
    response => response.status match
      case Status(s, _) if s >= 200 && s < 300 =>
        val json = response.body
        val parsed = parse(json).flatMap(_.as[A])
        parsed match
          case Left(error) => errorCB(s"Parsing error: $error")
          case Right(value) => valueCB(value)

      case Status(s, m) if s >= 400 && s < 600 =>
        errorCB(s"Error: $m")
}
