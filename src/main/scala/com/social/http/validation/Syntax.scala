package com.social.http.validation

import cats.*
import cats.implicits.*
import cats.data.*
import cats.data.Validated.*
import com.social.http.responses.Responses.*
//import com.social.http.responses.Responses.FailureResponse
import com.social.http.validation.Validators.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import com.social.logging.Syntax.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder


object Syntax {

  def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_]](using mt: MonadThrow[F], l: Logger[F]) extends Http4sDsl[F] {
    extension (req: Request[F])
      def validate[A: Validator](serverLogicIfValid: A => F[Response[F]])
                                (using EntityDecoder[F, A]): F[Response[F]] =
        req
          .as[A]
          .logError(e => s"Parsing payload failed: $e")
          .map(validateEntity)
          .flatMap { (validationResult: ValidationResult[A]) => validationResult match
            case Valid(a) => serverLogicIfValid(a)
            case Invalid(e) => BadRequest(FailureResponse(e.toList.map(_.msg).mkString(", ")))
          }
  }
}
