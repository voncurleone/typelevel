package com.social.modules

import cats.effect.*
import cats.implicits.*
import com.social.config.SecurityConfig
import com.social.core.{Auth, LiveAuth, LivePosts, LiveUsers, Posts, Users}
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

final class Core[F[_]] private (val posts: Posts[F], val auth: Auth[F], val users: Users[F])

//postgre -> posts -> core -> httpApi -> app
object Core {

  def apply[F[_]: Async: Logger](xa: Transactor[F])(securityConfig: SecurityConfig): Resource[F, Core[F]] =
    val coreF = for {
      posts <- LivePosts[F](xa)
      users <- LiveUsers[F](xa)
      auth <- LiveAuth[F](users)
    } yield new Core(posts, auth, users)

    Resource.eval(coreF)

//    Resource.eval(LivePosts[F](xa))
//      .map(posts => new Core[F](posts))
}