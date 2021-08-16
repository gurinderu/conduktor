package io.conduktor.tracing

import cats.effect.{Concurrent, ContextShift, Resource, Timer}
import io.conduktor.config.TracingConfig
import io.conduktor.executors.ExecutionContexts
import io.janstenpickle.trace4cats.inject.EntryPoint
import io.janstenpickle.trace4cats.jaeger.JaegerSpanCompleter
import io.janstenpickle.trace4cats.kernel.SpanSampler
import io.janstenpickle.trace4cats.model.TraceProcess

object JaegerEntryPoint {
  def makeResource[I[_]: Concurrent: ContextShift: Timer](config: TracingConfig): Resource[I, EntryPoint[I]] =
    for {
      blocker <- ExecutionContexts.blocker[I]("span-completer-blocker", daemon = true)
      process = TraceProcess(config.serviceName.value)
      completer <-
        JaegerSpanCompleter[I](blocker, process, host = config.collectorHost.value, port = config.collectorPort.value)
      entryPoint = EntryPoint[I](SpanSampler.probabilistic[I](config.samplerRate.value.toDouble), completer)
    } yield entryPoint
}
