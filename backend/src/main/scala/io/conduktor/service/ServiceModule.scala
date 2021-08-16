package io.conduktor.service

import cats.effect.{Clock, Concurrent, ContextShift, Resource, Sync}
import derevo.derive
import io.conduktor.context.ServiceCtx
import io.conduktor.repo.RepoModule
import io.conduktor.util.syntax.resource._
import io.janstenpickle.trace4cats.inject.Trace
import tofu.{Fire, WithProvide}
import tofu.common.TimeZone
import tofu.doobie.transactor.Txr
import tofu.generate.GenUUID
import tofu.higherKind.derived.representableK
import tofu.lift.{Lift, UnliftIO}
import tofu.logging.{LoggingCompanion, Logs}
import tofu.syntax.logging._
import tofu.syntax.handle._
import tofu.syntax.funk.funKFrom
import cats.tagless.syntax.functorK._
import io.conduktor.service.cluster.ClusterService
import io.conduktor.service.topic.TopicService

@derive(representableK)
trait ServiceModule[F[_]] {
  def cluster: ClusterService[F]

  def topic: TopicService[F]
}

object ServiceModule extends LoggingCompanion[ServiceModule] {
  def makeResourceIn[
    //@formatter:off
    I[_] : Sync : Lift[*[_], F],
    F[_] : Concurrent : ContextShift : Clock : Fire : Logs[I, *[_]]  : UnliftIO : Trace,
    DB[_] : Sync : GenUUID : Clock : TimeZone : Txr[F, *[_]]
    //@formatter:on
  ](
    repos: RepoModule[DB]
  )(implicit WP: WithProvide[F, I, ServiceCtx[I]]): Resource[I, ServiceModule[F]] =
    for {
      implicit0(log: Log[F]) <- Logs[I, F].service[ServiceModule[Any]].toResource
      errorHandler = funKFrom[F](_.onError((ex: Throwable) => errorCause"Unexpected error" (ex)))
      cluster <- ClusterService.makeEffectIn[I, F, DB](repos.cluster).toResource
      topic   <- TopicService.makeEffectIn[I, F, DB](repos.cluster).toResource
      impl = Impl(cluster, topic): ServiceModule[F]
    } yield impl.mapK(errorHandler)

  final case class Impl[F[_]](cluster: ClusterService[F], topic: TopicService[F]) extends ServiceModule[F]
}
