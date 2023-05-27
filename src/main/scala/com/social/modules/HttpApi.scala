package com.social.modules

import cats.*
import cats.effect.Resource
import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.social.http.routes.{HealthRoutes, PostRoutes, AuthRoutes}
import com.social.modules.Core
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F]) {
  private val healthRoutes = HealthRoutes[F].routes
  private val postRoutes = PostRoutes[F](core.posts).routes
  private val authRoutes = AuthRoutes[F](core.auth).routes

  val endPoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> postRoutes <+> authRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger ](core: Core[F]): Resource[F, HttpApi[F]] =
    Resource.pure(new HttpApi[F](core))
}
