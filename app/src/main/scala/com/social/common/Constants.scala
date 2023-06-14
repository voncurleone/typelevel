package com.social.common

object Constants {
  val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  object Endpoints {
    val root = "http://localhost:4041"
    val signup = s"$root/api/auth/users"
    val login = s"$root/api/auth/login"
  }

  object cookies {
    val duration = 10 * 24 * 3600 * 1000 //10 days in millis
    val email = "email"
    val token = "token"
  }
}
