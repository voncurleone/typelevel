package com.social

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.ember.server.EmberServerBuilder
import cats.*
import cats.effect.*
import cats.effect.{IO, IOApp}
import com.social.config.EmberConfig
import com.social.config.syntax.*
import com.social.http.routes.HealthRoutes
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException


object Application extends IOApp.Simple {

  private val configSource = ConfigSource.default.load[EmberConfig]

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
    EmberServerBuilder
      .default[IO]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(HealthRoutes[IO].routes.orNotFound)
      .build
      .use(_ => IO(println("server running...")) *> IO.never)
  }

}
