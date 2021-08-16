package io.conduktor.streaming.consumer.model

import derevo.derive
import tofu.logging.derivation.loggable

import java.time.Instant

@derive(loggable)
final case class ConsumeResult(
  time: Instant,
  key: String,
  value: String
)
