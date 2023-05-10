package com.social.fixtures

import com.social.domain.user.*

trait UserFixture {
  val person = User(
    "person@domain.com",
    "person",
    "password",
    Some("per"),
    Some("son"),
    Role.USER
  )

  val personEmail = "person@domain.com"

  val updatedPerson = User(
    "person@domain.com",
    "person",
    "updated pass",
    Some("per"),
    Some("son"),
    Role.USER
  )

  val admin = User(
    "admin@domain.com",
    "admin",
    "secure password",
    Some("ad"),
    Some("min"),
    Role.ADMIN
  )

  val adminEmail = "admin@domain.com"
  
  val newUser = User(
    "newuser@domain.com",
    "newuser",
    "pass",
    None,
    None,
    Role.USER
  )
}
