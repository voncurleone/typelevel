package com.social.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import com.social.domain.Post.PostInfo

import java.net.URL
import scala.util.{Failure, Success, Try}

object Validators {

  sealed trait ValidationFailure(val msg: String)
  case class EmptyField(fieldName: String) extends ValidationFailure(s"$fieldName is empty.")
  case class InvalidUrl(fieldName: String) extends ValidationFailure(s"$fieldName is not a valid Url.")

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  def validateRequired[A](field: A, fieldName: String)(required: A => Boolean): ValidationResult[A] =
    if required(field) then field.validNel
    else EmptyField(fieldName).invalidNel

  def validatUrl(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI) match {
      case Success(_) => field.validNel
      case Failure(_) => InvalidUrl(fieldName).invalidNel
    }

  given postInfoValidator: Validator[PostInfo] = (postInfo: PostInfo) => {
    val PostInfo(
      text,
      likes,
      disLikes,
      tags,
      image
    ) = postInfo

    val validText = validateRequired(text, "text")(_.nonEmpty)
    val validLikes = validateRequired(likes, "likes")(_.isValidInt)
    val validDisLikes = validateRequired(disLikes, "disLikes")(_.isValidInt)

    (
      validText,
      validLikes,
      validDisLikes,
      tags.validNel,
      image.validNel
    ).mapN(PostInfo.apply)
  }
}
