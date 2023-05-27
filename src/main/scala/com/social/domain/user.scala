package com.social.domain

import doobie.Meta
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

object user {
  final case class User(
      email: String,
      handle: String,
      hashedPass: String,
      firstName: Option[String],
      lastName: Option[String],
      role: Role
                 )

  final case class NewUserInfo(
      email: String,
      handle: String,
      pass: String,
      firstName: Option[String],
      lastName: Option[String],
                          )

  enum Role {
    case ADMIN, USER, GUEST
  }

  object Role {
    given metaRole: Meta[Role] =
      Meta[String].timap[Role](Role.valueOf)(_.toString)

    given roleAuthEnum: SimpleAuthEnum[Role, String] with {
      override val values: AuthGroup[Role] = AuthGroup(Role.USER, Role.ADMIN, Role.GUEST)
      override def getRepr(role: Role): String = role.toString
    }
  }
}
