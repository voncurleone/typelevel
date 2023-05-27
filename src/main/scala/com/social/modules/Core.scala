package com.social.modules

import cats.effect.*
import cats.implicits.*
import com.social.config.SecurityConfig
import com.social.core.{Auth, LiveAuth, LivePosts, LiveUsers, Posts}
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

final class Core[F[_]] private (val posts: Posts[F], val auth: Auth[F])

//postgre -> posts -> core -> httpApi -> app
object Core {

  def apply[F[_]: Async: Logger](xa: Transactor[F])(securityConfig: SecurityConfig): Resource[F, Core[F]] =
    val coreF = for {
      posts <- LivePosts[F](xa)
      users <- LiveUsers[F](xa)
      auth <- LiveAuth[F](users)(securityConfig)
    } yield new Core(posts, auth)

    Resource.eval(coreF)

//    Resource.eval(LivePosts[F](xa))
//      .map(posts => new Core[F](posts))
}