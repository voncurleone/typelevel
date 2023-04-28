package com.social.modules

import cats.effect.*
import cats.implicits.*
import com.social.core.{LivePosts, Posts}
import doobie.util.transactor.Transactor

final class Core[F[_]] private (val posts: Posts[F])

//postgre -> posts -> core -> httpApi -> app
object Core {

  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource.eval(LivePosts[F](xa))
      .map(posts => new Core[F](posts))
}