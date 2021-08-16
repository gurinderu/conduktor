package io.conduktor.streaming.consumer.model

import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.conduktor.enums.{Format, Offset}
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import tofu.logging.derivation.loggable
import tofu.logging.refined._

@derive(loggable)
final case class ConsumeParams(
  clusterId: ClusterId,
  topicName: NonEmptyString,
  keyFormat: Format,
  valueFormat: Format,
  offset: Offset
)
