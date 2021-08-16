package io.conduktor.config

import cats.instances.option._
import cats.instances.string._
import derevo.derive
import derevo.pureconfig.config
import eu.timepit.refined.api.Refined
import eu.timepit.refined.cats._
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.{NonEmptyFiniteString, NonEmptyString}
import io.conduktor.config.{DbConfig, GraphQLConfig, TracingConfig}
import tofu.logging.derivation.{loggable, show}
import tofu.logging.refined._

@derive(config, show, loggable)
final case class AppConfig(
  name: String,
  hostname: NonEmptyFiniteString[255],
  domainName: NonEmptyString,
  baseUrl: String Refined Url,
  database: DbConfig,
  graphql: GraphQLConfig,
  tracing: Option[TracingConfig]
)
