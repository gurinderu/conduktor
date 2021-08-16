package io.conduktor.streaming.consumer

import cats.Monad
import cats.effect.{Clock, ConcurrentEffect, ContextShift, Timer}
import cats.syntax.show._
import derevo.derive
import fs2.Stream
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, Deserializer, KafkaConsumer}
import io.conduktor.enums.{Format, Offset}
import io.conduktor.repo.ClusterRepo
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import io.conduktor.service.ServiceError
import io.conduktor.streaming.consumer.model.{ConsumeParams, ConsumeResult}
import tofu.common.TimeZone
import tofu.doobie.transactor.Txr
import tofu.lift.Lift
import tofu.logging.{LoggingCompanion, Logs}
import tofu.syntax.time._
import tofu.syntax.monadic._
import tofu.syntax.doobie.txr._
import tofu.syntax.foption._
import io.circe.syntax._
import io.conduktor.repo.model.cluster.Cluster
import tofu.higherKind.derived.representableK

import java.time.Instant

@derive(representableK)
trait ConsumerService[S[_]] {
  def consume(params: ConsumeParams): S[ConsumeResult]
}

object ConsumerService extends LoggingCompanion[ConsumerService] {
  sealed abstract class Errors(override val getMessage: String) extends ServiceError

  object Errors {
    final case class ClusterNotFound(id: ClusterId) extends Errors(show"Could not find cluster by id $id")
  }

  type Handle[F[_]] = tofu.Handle[F, Errors]
  type Raise[F[_]]  = tofu.Raise[F, Errors]

  def makeEffect[
    I[_]: ConcurrentEffect: ContextShift: Timer,
    F[_]: Monad: Timer: Clock: TimeZone: Logs[I, *[_]]: Lift[I, *[_]],
    DB[_]: Monad: Raise: Txr[F, *[_]]
  ](
    clusterRepo: ClusterRepo[DB]
  ): I[ConsumerService[fs2.Stream[F, *]]] =
    for {
      implicit0(logging: Log[F]) <- Logs[I, F].service[ConsumerService[Any]]
      impl: ConsumerService[fs2.Stream[F, *]] = Impl[I, F, DB](
        clusterRepo = clusterRepo
      )
    } yield impl

  case class Impl[
    I[_]: ConcurrentEffect: ContextShift: Timer,
    F[_]: Monad: Clock: TimeZone: Lift[I, *[_]],
    DB[_]: Monad: Raise: Txr[F, *[_]]
  ](clusterRepo: ClusterRepo[DB])
      extends ConsumerService[fs2.Stream[F, *]] {
    private def deserializer(format: Format): Deserializer[I, String] = format match {
      case Format.`String` => Deserializer.string[I]
      case Format.`JSON`   => Deserializer.string[I].map(_.asJson.show)
    }

    private def offset(offset: Offset): AutoOffsetReset = offset match {
      case Offset.Earliest => AutoOffsetReset.Earliest
      case Offset.Latest   => AutoOffsetReset.Latest
    }

    def consume(params: ConsumeParams): Stream[F, ConsumeResult] =
      for {
        now <- Stream.eval(now[F, Instant])
        cluster <- Stream.eval[F, Cluster](
          clusterRepo.fetchById(params.clusterId).orThrow(Errors.ClusterNotFound(params.clusterId)).trans
        )
        consumerSettings: ConsumerSettings[I, String, String] = ConsumerSettings(
          keyDeserializer = deserializer(params.keyFormat),
          valueDeserializer = deserializer(params.valueFormat)
        ).withAutoOffsetReset(offset(params.offset))
          .withBootstrapServers(cluster.bootstrapServers.value)
          .withProperties(cluster.properties)
        res <- KafkaConsumer
          .stream[I, String, String](consumerSettings)
          .evalTap(_.assign(params.topicName.value))
          .flatMap(_.stream)
          .translate(Lift[I, F].liftF)
      } yield ConsumeResult(
        now,
        res.record.key,
        res.record.value
      )

  }

}
