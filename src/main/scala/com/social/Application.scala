package com.social

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.ember.server.EmberServerBuilder
import cats.*
import cats.effect.*
import cats.effect.{IO, IOApp}
import com.social.http.routes.HealthRoutes


object Application extends IOApp.Simple {

  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
    .withHttpApp(HealthRoutes[IO].routes.orNotFound)
    .build
    .use(_ => IO(println("server running...")) *> IO.never)
}
