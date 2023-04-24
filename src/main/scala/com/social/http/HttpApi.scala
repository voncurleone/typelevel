package com.social.http

import cats.*
import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.social.http.routes.{HealthRoutes, PostRoutes}
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val postRoutes = PostRoutes[F].routes

  val endPoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> postRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger ] = new HttpApi[F]
}
