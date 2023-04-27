package com.social.modules

import cats.effect.*
import cats.implicits.*
import com.social.core.{LivePosts, Posts}
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor

final class Core[F[_]] private (val posts: Posts[F])

//postgre -> posts -> core -> httpApi -> app
object Core {
  def postgreResource[F[_]: Async]: Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[F](
      "org.postgresql.Driver", //todo: move to config
      "jdbc:postgresql:social",
      "docker",
      "docker",
      ec
    )
  } yield xa

  def apply[F[_]: Async]: Resource[F, Core[F]] =
    postgreResource[F]
      .evalMap( postgre => LivePosts[F](postgre))
      .map(posts => new Core[F](posts))
}