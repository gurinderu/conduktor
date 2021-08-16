package io.conduktor.config

import cats.instances.float._
import cats.instances.int._
import cats.instances.string._
import derevo.derive
import derevo.pureconfig.config
import eu.timepit.refined.cats._
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.all.NonEmptyString
import eu.timepit.refined.types.net.PortNumber
import eu.timepit.refined.types.numeric.NonNegFloat
import tofu.logging.derivation.{loggable, show}
import tofu.logging.refined._

@derive(config, show, loggable)
final case class TracingConfig(
  serviceName: NonEmptyString,
  collectorHost: NonEmptyString,
  collectorPort: PortNumber,
  samplerRate: NonNegFloat
)
