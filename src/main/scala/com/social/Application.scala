package com.social

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.ember.server.EmberServerBuilder
import cats.*
import cats.effect.*
import cats.effect.{IO, IOApp}
import com.social.config.{AppConfig, EmberConfig}
import com.social.config.Syntax.*
import com.social.http.routes.HealthRoutes
import com.social.modules.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import cats.effect.IO.asyncForIO


object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig, securityConfig, tokenConfig, emailServiceConfig) =>
      val appResource = for {
        xa <- Database.makePostgresResource(postgresConfig)
        core <-  Core[IO](xa)(tokenConfig, emailServiceConfig)
        httpApi <- HttpApi[IO](core, securityConfig)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(httpApi.endPoints.orNotFound)
          .build
      } yield server

      appResource.use(_ => IO(println("server running...")) *> IO.never)
  }

}
