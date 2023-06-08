package com.social.domain

object auth {
  final case class LoginInfo(email: String, password: String)
  final case class NewPasswordInfo(oldPassword: String, newPassword: String)
  final case class ForgotPasswordInfo(email: String)
  final case class RecoverPasswordInfo(email: String, token: String, newPassword: String)

  final case class NewUserInfo(
                                email: String,
                                handle: String,
                                pass: String,
                                firstName: Option[String],
                                lastName: Option[String],
                              )
}
