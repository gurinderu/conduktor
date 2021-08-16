package io.conduktor.service.cluster.model

import derevo.derive
import eu.timepit.refined.types.string.NonEmptyFiniteString
import tofu.logging.derivation.loggable
import tofu.logging.refined._

@derive(loggable)
final case class CreateClusterParams(
  name: NonEmptyFiniteString[255],
  bootstrapServers: NonEmptyFiniteString[255],
  properties: Map[String, String]
)
