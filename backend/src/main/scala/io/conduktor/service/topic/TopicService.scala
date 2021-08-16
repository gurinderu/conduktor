package io.conduktor.service.topic

import cats.effect.{Concurrent, ContextShift}
import cats.syntax.show._
import cats.{Functor, Monad}
import derevo.derive
import fs2.kafka.{AdminClientSettings, KafkaAdminClient}
import io.conduktor.repo.ClusterRepo
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import io.conduktor.service.ServiceError
import io.conduktor.service.topic.model.Topic
import io.conduktor.util.syntax.mid._
import io.janstenpickle.trace4cats.inject.Trace
import io.janstenpickle.trace4cats.inject.syntax._
import tofu.doobie.transactor.Txr
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{LoggingCompanion, Logs}
import tofu.syntax.doobie.txr._
import tofu.syntax.foption._
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait TopicService[F[_]] {
  def fetchByClusterId(id: ClusterId): F[List[Topic]]
}

object TopicService extends LoggingCompanion[TopicService] {
  sealed abstract class Errors(override val getMessage: String) extends ServiceError

  object Errors {
    final case class ClusterNotFound(id: ClusterId) extends Errors(show"Could not find cluster by id $id")
  }

  type Handle[F[_]] = tofu.Handle[F, Errors]
  type Raise[F[_]]  = tofu.Raise[F, Errors]

  def makeEffectIn[
    I[_]: Functor,
    F[_]: Concurrent: ContextShift: Trace: Logs[I, *[_]],
    DB[_]: Monad: Raise: Txr[F, *[_]]
  ](
    clusterRepo: ClusterRepo[DB]
  ): I[TopicService[F]] =
    for {
      implicit0(log: Log[F]) <- Logs[I, F].service[TopicService[Any]]
      mid = new TracingMid[F] >>> new LoggingMid[F]
      impl = new Impl[F, DB](
        clusterRepo = clusterRepo
      )
    } yield mid attach impl

  final class TracingMid[F[_]: Trace] extends TopicService[Mid[F, *]] {
    private val prefix = "TopicService"

    def fetchByClusterId(id: ClusterId): Mid[F, List[Topic]] = _.span(s"$prefix.fetchByClusterId")
  }

  final class LoggingMid[F[_]: Monad: Log] extends TopicService[Mid[F, *]] {

    def fetchByClusterId(id: ClusterId): Mid[F, List[Topic]] =
      debug"Fetching topics by cluster id $id" *> _
  }

  final class Impl[F[_]: Concurrent: ContextShift, DB[_]: Monad: Txr[F, *[_]]: Raise](clusterRepo: ClusterRepo[DB])
      extends TopicService[F] {
    def fetchByClusterId(id: ClusterId): F[List[Topic]] =
      for {
        cluster <- clusterRepo.fetchById(id).orThrow(Errors.ClusterNotFound(id)).trans
        res <- KafkaAdminClient
          .resource[F](AdminClientSettings[F]
            .withBootstrapServers(cluster.bootstrapServers.value)
            .withProperties(cluster.properties)
          )
          .use { client =>
            for {
              listings <- client.listTopics.listings
            } yield listings.map(l => Topic(l.name()))
          }
      } yield res
  }

}
