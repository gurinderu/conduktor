package io.conduktor.service.cluster

import cats.{Functor, Monad}
import cats.syntax.show._
import derevo.derive
import io.conduktor.repo.ClusterRepo
import io.conduktor.repo
import io.conduktor.repo.model.cluster.Cluster
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import io.conduktor.service.ServiceError
import io.conduktor.service.cluster.model.CreateClusterParams
import io.conduktor.util.syntax.mid._
import io.janstenpickle.trace4cats.inject.Trace
import tofu.doobie.transactor.Txr
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{LoggingCompanion, Logs}
import tofu.syntax.monadic._
import tofu.syntax.logging._
import tofu.syntax.foption._
import tofu.syntax.doobie.txr._
import io.janstenpickle.trace4cats.inject.syntax._
import io.scalaland.chimney.dsl._

@derive(representableK)
trait ClusterService[F[_]] {
  def fetchById(id: ClusterId): F[Option[Cluster]]

  def unsafeFetchById(id: ClusterId): F[Cluster]

  def fetchAll(): F[List[Cluster]]

  def create(params: CreateClusterParams): F[ClusterId]
}

object ClusterService extends LoggingCompanion[ClusterService] {
  sealed abstract class Errors(override val getMessage: String) extends ServiceError

  object Errors {
    final case class ClusterNotFound(id: ClusterId) extends Errors(show"Could not find cluster by id $id")
  }

  type Handle[F[_]] = tofu.Handle[F, Errors]
  type Raise[F[_]]  = tofu.Raise[F, Errors]

  def makeEffectIn[
    I[_]: Functor,
    F[_]: Monad: Trace: Logs[I, *[_]],
    DB[_]: Monad: Raise: Txr[F, *[_]]
  ](
    clusterRepo: ClusterRepo[DB]
  ): I[ClusterService[F]] =
    for {
      implicit0(log: Log[F]) <- Logs[I, F].service[ClusterService[Any]]
      mid = new TracingMid[F] >>> new LoggingMid[F]
      impl = new Impl[F, DB](
        clusterRepo = clusterRepo
      )
    } yield mid attach impl

  final class TracingMid[F[_]: Trace] extends ClusterService[Mid[F, *]] {
    private val prefix = "ClusterService"

    def create(params: CreateClusterParams): Mid[F, ClusterId] = _.span(s"$prefix.create")

    def fetchAll(): Mid[F, List[Cluster]] = _.span(s"$prefix.fetchAll")

    def fetchById(id: ClusterId): Mid[F, Option[Cluster]] =
      _.span(s"$prefix.fetchById")

    def unsafeFetchById(id: ClusterId): Mid[F, Cluster] =
      _.span(s"$prefix.unsafeFetchById")
  }

  final class LoggingMid[F[_]: Monad: Log] extends ClusterService[Mid[F, *]] {
    def create(params: CreateClusterParams): Mid[F, ClusterId] =
      res =>
        info"Creating cluster with params $params" *>
          res.flatTap(id => info"Cluster with ID = $id created.")

    def fetchAll(): Mid[F, List[Cluster]] =
      debug"Fetching all clusters" *> _

    def fetchById(id: ClusterId): Mid[F, Option[Cluster]] =
      res => debug"Fetching cluster by id $id" *> res.flatTap(r => debug"Fetched ${r.size}")

    def unsafeFetchById(id: ClusterId): Mid[F, Cluster] =
      debug"Fetching cluster by id $id" *> _
  }

  final class Impl[F[_]: Monad, DB[_]: Monad: Txr[F, *[_]]: Raise](clusterRepo: ClusterRepo[DB])
      extends ClusterService[F] {
    def create(params: CreateClusterParams): F[ClusterId] = {
      val tx = clusterRepo.insert(params.into[repo.model.cluster.CreateClusterParams].transform)
      tx.trans
    }

    def fetchAll(): F[List[Cluster]] = clusterRepo.fetchAll().trans

    def fetchById(id: ClusterId): F[Option[Cluster]] = clusterRepo.fetchById(id).trans

    def unsafeFetchById(id: ClusterId): F[Cluster] = clusterRepo.fetchById(id).orThrow(Errors.ClusterNotFound(id)).trans
  }

}
