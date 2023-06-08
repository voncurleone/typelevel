package com.social.common

import cats.effect.IO
import io.circe.Encoder
import io.circe.syntax.*
import tyrian.Cmd
import tyrian.http.{Decoder, Http, HttpError, Method, Request, Response, Body}

trait Endpoint[M] {
  val location: String
  val method: Method
  val onSuccess: Response => M
  val onError: HttpError => M

  def call[A: Encoder](payload: A): Cmd[IO, M] =
    Http.send(
      Request(
        method,
        headers = List(),
        url = location,
        body = Body.json(payload.asJson.toString),
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onSuccess, onError)
    )
}
