package com.social.domain

object pagination {
  final case class Pagination(limit: Int, offset: Int)

  object Pagination {
    val defaultPageSize = 10

    def apply(limit: Option[Int], offset: Option[Int]): Pagination =
      new Pagination(
        limit.getOrElse(defaultPageSize),
        offset.getOrElse(0)
      )
  }
}