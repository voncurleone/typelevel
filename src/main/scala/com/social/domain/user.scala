package com.social.domain

import doobie.Meta

object user {
  final case class User(
      email: String,
      handle: String,
      hashedPass: String,
      firstName: Option[String],
      lastName: Option[String],
      role: Role
                 )
  enum Role {
    case ADMIN, USER, GUEST
  }

  object Role {
    given metaRole: Meta[Role] =
      Meta[String].timap[Role](Role.valueOf)(_.toString)
  }
}
