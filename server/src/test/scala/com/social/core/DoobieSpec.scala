package com.social.core

import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import org.testcontainers.containers.PostgreSQLContainer
import doobie.util.ExecutionContexts
import cats.effect.*
import cats.effect.IO.asyncForIO

trait DoobieSpec {
  //to be implemented by whatever test case interacts with the db
  val initScript: String

  //simulate a database with testContainers
  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = IO {
      val container: PostgreSQLContainer[Nothing] =
        new PostgreSQLContainer("postgres")
          .withInitScript(initScript)

      container.start()
      container
    }

    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())

    Resource.make(acquire)(release)
  }

  //setup a postgres transactor
  val transactor: Resource[IO, Transactor[IO]] = for {
    db <- postgres
    ce <- ExecutionContexts.fixedThreadPool[IO](1)
    xa <- HikariTransactor.newHikariTransactor(
      "org.postgresql.Driver",
      db.getJdbcUrl,
      db.getUsername,
      db.getPassword,
      ce
    )
  } yield xa
}
