package com.social.common

import cats.effect.IO
import com.social.core.Session
import io.circe.Encoder
import io.circe.syntax.*
import tyrian.Cmd
import tyrian.http.{Body, Decoder, Header, Http, HttpError, Method, Request, Response}

trait Endpoint[M] {
  val location: String
  val method: Method
  val onSuccess: Response => M
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
      Decoder[M](onSuccess, onError)
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
      Decoder[M](onSuccess, onError)
    )
}
