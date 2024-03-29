package com.social.common

object Constants {
  val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
  val defaultPageSize = 20

  object endpoints {
    val root = "http://localhost:4041"
    val signup = s"$root/api/auth/users"
    val login = s"$root/api/auth/login"
    val logout = s"$root/api/auth/logout"
    val checkToken = s"$root/api/auth/checkToken"
    val forgotPassword = s"$root/api/auth/reset"
    val resetPassword = s"$root/api/auth/recover"
    val profileReset = s"$root/api/auth/users/password"
    val makePost = s"$root/api/posts/create"
    val posts = s"$root/api/posts"
    val filters = s"$root/api/posts/filters"
  }

  object cookies {
    val duration = 10 * 24 * 3600 * 1000 //10 days in millis
    val email = "email"
    val token = "token"
  }
}
