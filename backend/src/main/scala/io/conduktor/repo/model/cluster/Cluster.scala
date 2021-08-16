package io.conduktor.repo.model.cluster

import derevo.cats.show
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyFiniteString
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import io.estatico.newtype.macros.newtype
import tofu.logging.derivation.loggable

import java.util.UUID

final case class Cluster(
  id: ClusterId,
  name: NonEmptyFiniteString[255],
  bootstrapServers: NonEmptyFiniteString[255],
  properties: Map[String, String]
)

object Cluster {
  @derive(loggable, show)
  @newtype final case class ClusterId(value: UUID)
}
