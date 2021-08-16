package io.conduktor.metrics

import cats.effect.Sync
import io.chrisdavenport.epimetheus.{Collector, CollectorRegistry}
import tofu.syntax.monadic._

final case class MetricsModule[I[_]](collectorRegistry: CollectorRegistry[I])

object MetricsModule {
  def makeEffect[I[_]: Sync]: I[MetricsModule[I]] = {
    val cr = CollectorRegistry.defaultRegistry[I]
    Collector.Defaults.registerDefaults(cr).as(MetricsModule(cr))
  }
}
