package com.social.modules

import cats.effect.*
import cats.implicits.*
import com.social.config.{EmailServiceConfig, SecurityConfig, TokenConfig}
import com.social.core.{Auth, LiveAuth, LivePosts, LiveTokens, LiveUsers, Posts, Users}
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

final class Core[F[_]] private (val posts: Posts[F], val auth: Auth[F], val users: Users[F])

//postgre -> posts -> core -> httpApi -> app
object Core {

  def apply[F[_]: Async: Logger](xa: Transactor[F])
          (tokenConfig: TokenConfig, emailServiceConfig: EmailServiceConfig): Resource[F, Core[F]] =
    val coreF = for {
      posts <- LivePosts[F](xa)
      users <- LiveUsers[F](xa)
      tokens <- LiveTokens[F](users)(xa, tokenConfig)
      emails <- LiveEmails[F](emailServiceConfig)
      auth <- LiveAuth[F](users, tokens, emails)
    } yield new Core(posts, auth, users)

    Resource.eval(coreF)

//    Resource.eval(LivePosts[F](xa))
//      .map(posts => new Core[F](posts))
}