package io.conduktor.config

import cats.instances.finiteDuration._
import cats.instances.int._
import derevo.derive
import derevo.pureconfig.config
import eu.timepit.refined.cats._
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.numeric.PosInt
import tofu.logging.derivation.{loggable, show}
import tofu.logging.refined._

import scala.concurrent.duration.FiniteDuration

@derive(config, show, loggable)
final case class GraphQLConfig(maxDepth: PosInt, maxFields: PosInt, timeout: FiniteDuration, slow: FiniteDuration)
