package com.social.fixtures

import com.social.domain.user.*

trait UserFixture {
  val person = User(
    "person@domain.com",
    "person",
    "$2a$10$0w0m2PIznul0M84rn6cRk.gUc28LrMJTHEM9UVzEMnjJdjtnrtE/C",
    Some("per"),
    Some("son"),
    Role.USER
  )
  
  val newUserPerson = NewUserInfo(
    "person@domain.com",
    "person",
    "password",
    Some("per"),
    Some("son")
  )

  val personEmail = "person@domain.com"
  val personPass = "password"

  val updatedPerson = User(
    "person@domain.com",
    "person",
    "$2a$10$b7vUYCcN.BpOKoBeVju5nOfydf3nGYPutXiQ/4Ix9CKvuhESMU8V.",
    Some("per"),
    Some("son"),
    Role.USER
  )

  val admin = User(
    "admin@domain.com",
    "admin",
    "$2a$10$bdA2ei/ZxQM4MEOxg1ihp.98Utpne1jLou2J8Yl4rRrndJ8LzznU6",
    Some("ad"),
    Some("min"),
    Role.ADMIN
  )
  
  val newUserAdmin = NewUserInfo(
    "admin@domain.com",
    "admin",
    "secure password",
    Some("ad"),
    Some("min")
  )

  val adminEmail = "admin@domain.com"
  val adminPass = "secure password"
  
  val newUser = User(
    "newuser@domain.com",
    "newuser",
    "$2a$10$MQXtG7RpZ085HgQIt0AQd.4xSdMTbSqudZ17vpF42x6w3hjaa6lRq",
    None,
    None,
    Role.USER
  )
}
