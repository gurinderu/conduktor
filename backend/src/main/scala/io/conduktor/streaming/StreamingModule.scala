package io.conduktor.streaming

import cats.Monad
import cats.effect.{Clock, ConcurrentEffect, ContextShift, Timer}
import derevo.derive
import io.conduktor.repo.RepoModule
import io.conduktor.streaming.consumer.ConsumerService
import tofu.Errors
import tofu.common.TimeZone
import tofu.doobie.transactor.Txr
import tofu.higherKind.derived.representableK
import tofu.lift.Lift
import tofu.syntax.monadic._
import tofu.logging.Logs

@derive(representableK)
trait StreamingModule[S[_]] {
  def consumer: ConsumerService[S]
}

object StreamingModule {
  def makeEffectIn[
    I[_]: ConcurrentEffect: ContextShift: Timer,
    F[_]: Monad: Timer: Clock: TimeZone: Logs[I, *[_]]: Lift[I, *[_]],
    DB[_]: Monad: Txr[F, *[_]]: Errors[*[_], Throwable]
  ](
    repoModule: RepoModule[DB]
  ): I[StreamingModule[fs2.Stream[F, *]]] =
    for {
      consumer <- ConsumerService.makeEffect[I, F, DB](repoModule.cluster)
    } yield Impl(consumer)

  case class Impl[S[_]](
    consumer: ConsumerService[S]
  ) extends StreamingModule[S]
}
