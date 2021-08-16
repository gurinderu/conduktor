package io.conduktor.tracing

import io.conduktor.config.TracingConfig
import io.conduktor.context.ServiceCtx
import io.conduktor.util.tracing.shims._
import cats.effect.{Concurrent, ContextShift, Resource, Sync, Timer}
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.inject.{EntryPoint, Trace}
import io.janstenpickle.trace4cats.inject.Trace.Implicits.{noop => noopTracing}
import tofu.WithLocal
import tofu.lift.Lift
import tofu.optics.Contains

final case class TracingModule[I[_], F[_], DB[_]](
  entryPoint: EntryPoint[I],
  tracingF: Trace[F],
  tracingDB: Trace[DB]
)

object TracingModule {
  def makeResourceIn[
    //@formatter:off
    I[_] : Concurrent : ContextShift : Timer,
    F[_] : Sync : Lift[I, *[_]] : *[_] WithLocal ServiceCtx[I],
    DB[_] : Sync : Lift[I, *[_]] : *[_] WithLocal ServiceCtx[I]
    //@formatter:on
  ](config: Option[TracingConfig]): Resource[I, TracingModule[I, F, DB]] = {
    val spanLens                                      = Contains[ServiceCtx[I], Span[I]]
    implicit val fHasLocalSpan: F WithLocal Span[I]   = WithLocal[F, ServiceCtx[I]].subcontext(spanLens)
    implicit val dbHasLocalSpan: DB WithLocal Span[I] = WithLocal[DB, ServiceCtx[I]].subcontext(spanLens)
    config
      .fold {
        Resource.pure[I, TracingModule[I, F, DB]](TracingModule(EntryPoint.noop[I], noopTracing[F], noopTracing[DB]))
      } { cfg =>
        JaegerEntryPoint.makeResource[I](cfg).map { ep =>
          TracingModule(ep, Trace.localSpanInstance[I, F], Trace.localSpanInstance[I, DB])
        }
      }
  }
}
