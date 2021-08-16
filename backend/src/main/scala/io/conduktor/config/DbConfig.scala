package io.conduktor.config

import cats.instances.boolean._
import cats.instances.finiteDuration._
import cats.instances.int._
import cats.instances.list._
import cats.instances.option._
import cats.instances.string._
import derevo.derive
import derevo.pureconfig.config
import tofu.logging.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.all.NonEmptyString
import eu.timepit.refined.types.numeric._
import io.conduktor.util.types.Secret
import tofu.logging.derivation.{loggable, show}

import scala.concurrent.duration.FiniteDuration

@derive(config, show, loggable)
final case class DbConfig(
  connection: DbConfig.Connection,
  driverClassName: NonEmptyString,
  hikari: Option[DbConfig.Hikari],
  flyway: Option[DbConfig.Flyway]
)

object DbConfig {
  @derive(config, show, loggable)
  final case class Connection(url: NonEmptyString, username: NonEmptyString, password: Secret)

  @derive(config, show, loggable)
  final case class Flyway(
    locations: List[String] = List("db/migration"),
    migrationsEnabled: Boolean = false,
    cleanDisabled: Boolean = true
  )

  @derive(config, show, loggable)
  final case class Hikari(
    connectionTimeout: Option[FiniteDuration],
    maximumPoolSize: Option[PosInt],
    maxLifetime: Option[FiniteDuration],
    registerMbeans: Option[Boolean]
  )
}
