package io.conduktor.context

import cats.{~>, Applicative, Defer}
import io.janstenpickle.trace4cats.Span
import tofu.lift.Lift
import tofu.optics.Extract
import tofu.optics.macros.ClassyOptics

@ClassyOptics
final case class ServiceCtx[F[_]](requestId: String, span: Span[F]) {
  self =>
  def mapK[G[_]: Defer: Applicative](fk: F ~> G): ServiceCtx[G] =
    self.copy(span = span.mapK(fk))

  def lift[G[_]: Lift[F, *[_]]: Defer: Applicative]: ServiceCtx[G] = self.mapK(Lift.trans[F, G])
}

object ServiceCtx {
  implicit def extractLogCtx[F[_]]: Extract[ServiceCtx[F], LogCtx] = s => LogCtx(s.requestId, s.span.context.traceId)
}
