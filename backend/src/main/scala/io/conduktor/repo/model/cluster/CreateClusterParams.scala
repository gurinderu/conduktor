package io.conduktor.repo.model.cluster

import eu.timepit.refined.types.string.NonEmptyFiniteString

final case class CreateClusterParams(
  name: NonEmptyFiniteString[255],
  bootstrapServers: NonEmptyFiniteString[255],
  properties: Map[String, String]
)
