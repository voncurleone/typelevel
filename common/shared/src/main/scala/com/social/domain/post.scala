package com.social.domain

import java.util.UUID

object post {
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
    def minimal(text: String, likes: Int, disLikes: Int): PostInfo = 
      PostInfo(text, likes, disLikes, None, None)
  }

  final case class PostFilter(
      text: List[String] = List(),
      likes: Option[Int] = None,
      dislikes: Option[Int] = None,
      tags: List[String] = List(),
      hidden: Boolean = false
                             )
}
