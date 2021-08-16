package io.conduktor.context

import cats.syntax.show._
import derevo.derive
import io.janstenpickle.trace4cats.model.TraceId
import tofu.logging.Loggable
import tofu.logging.derivation.loggable

@derive(loggable)
final case class LogCtx(requestId: String, traceId: TraceId)
object LogCtx {
  implicit val traceIdLoggable: Loggable[TraceId] = Loggable[String].contramap(_.show)
}
