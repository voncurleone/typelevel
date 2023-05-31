package com.social.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import com.social.domain.auth.{LoginInfo, NewPasswordInfo}
import com.social.domain.post.PostInfo
import com.social.domain.user.NewUserInfo

import java.net.URL
import scala.util.{Failure, Success, Try}

object Validators {

  sealed trait ValidationFailure(val msg: String)
  case class EmptyField(fieldName: String) extends ValidationFailure(s"$fieldName is empty.")
  case class InvalidUrl(fieldName: String) extends ValidationFailure(s"$fieldName is not a valid Url.")
  case class InvalidEmail(fieldName: String) extends ValidationFailure(s"$fieldName is invalid.")

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  def validateRequired[A](field: A, fieldName: String)(required: A => Boolean): ValidationResult[A] =
    if required(field) then field.validNel
    else EmptyField(fieldName).invalidNel

  def validateUrl(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI) match {
      case Success(_) => field.validNel
      case Failure(_) => InvalidUrl(fieldName).invalidNel
    }

  def validateEmail(field: String, fieldName: String): ValidationResult[String] =
    val emailRegex =
      """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    if emailRegex.findFirstMatchIn(field).isDefined then field.validNel
    else InvalidEmail("Email").invalidNel

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

  given loginInfoValidator: Validator[LoginInfo] = (loginInfo: LoginInfo) => {
    val validUserEmail = validateRequired(loginInfo.email, "email")(_.nonEmpty)
      .andThen(email => validateEmail(email, "email"))
    val validUserPass = validateRequired(loginInfo.password, "password")(_.nonEmpty)

    (validUserEmail, validUserPass).mapN(LoginInfo.apply)
  }

  given newUserInfoValidator: Validator[NewUserInfo] = (newUserInfo: NewUserInfo) => {
    val validUserEmail = validateRequired(newUserInfo.email, "email")(_.nonEmpty)
      .andThen(email => validateEmail(email, "email"))
    val validUserPass = validateRequired(newUserInfo.pass, "password")(_.nonEmpty)
    //todo: can run more password validation logic here

    (
      validUserEmail,
      newUserInfo.handle.validNel,
      validUserPass,
      newUserInfo.firstName.validNel,
      newUserInfo.lastName.validNel
    ).mapN(NewUserInfo.apply)
  }

  given newPasswordInfoValidator: Validator[NewPasswordInfo] = (newPasswordInfo: NewPasswordInfo) => {
    val validOldPass = validateRequired(newPasswordInfo.oldPassword, "old password")(_.nonEmpty)
    val validNewPass = validateRequired(newPasswordInfo.oldPassword, "new password")(_.nonEmpty)
    //todo: can run more password validation logic here

    (validOldPass, validNewPass).mapN(NewPasswordInfo.apply)
  }
}
