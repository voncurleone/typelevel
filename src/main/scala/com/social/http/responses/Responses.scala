package com.social.http.responses

import cats.*
import org.http4s.EntityEncoder
import io.circe.generic.auto._
import org.http4s.circe.jsonEncoderOf


object Responses {
  final case class FailureResponse(error: String)

  given [F[_]: Applicative]: EntityEncoder[F, FailureResponse] = jsonEncoderOf[F, FailureResponse]
}