package com.social.domain

import java.util.UUID

object Post {
  case class Post(
                 id: UUID,
                 date: Long,
                 email: String,
                 postInfo: PostInfo,
                 hidden: Boolean = false
                 )

  case class PostInfo(
                     text: String,
                     likes: Int,
                     disLikes: Int,
                     tags: Option[List[String]],
                     image: Option[String]
                     )

  object PostInfo {
    val empty: PostInfo = PostInfo("", 0, 0, None, None)
  }
}
