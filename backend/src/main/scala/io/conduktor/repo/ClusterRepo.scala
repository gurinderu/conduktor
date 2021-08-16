package io.conduktor.repo

import cats.{Functor, Monad}
import derevo.derive
import doobie.ConnectionIO
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import io.conduktor.repo.model.cluster.{Cluster, CreateClusterParams}
import io.conduktor.repo.query.ClusterSQL
import io.conduktor.util.doobie.logHandler
import io.janstenpickle.trace4cats.inject.Trace
import tofu.doobie.LiftConnectionIO
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.lift.UnliftIO
import io.janstenpickle.trace4cats.inject.syntax._
import tofu.logging.{LoggingCompanion, Logs}
import tofu.syntax.monadic._

@derive(representableK)
trait ClusterRepo[DB[_]] {
  def fetchAll(): DB[List[Cluster]]

  def fetchById(id: ClusterId): DB[Option[Cluster]]

  def insert(params: CreateClusterParams): DB[ClusterId]
}

object ClusterRepo extends LoggingCompanion[ClusterRepo] {
  def makeEffectIn[
    I[_]: Functor,
    G[_],
    DB[_]: Monad: LiftConnectionIO: UnliftIO: Logs[I, *[_]]: Trace
  ](
    sqlr: G[ClusterSQL]
  )(implicit G: ProvideLogHandler[G]): I[ClusterRepo[DB]] =
    for {
      implicit0(logging: Log[DB]) <- Logs[I, DB].of[ClusterRepo]
      elh  = logHandler.makeEmbeddable
      mid  = new TracingMid[DB]
      impl = elh.embedLift(lh => new Impl(G.runContext(sqlr)(lh)))
    } yield mid attach impl

  final class TracingMid[DB[_]: Trace] extends ClusterRepo[Mid[DB, *]] {
    private val prefix = "ClusterRepo"

    def insert(params: CreateClusterParams): Mid[DB, ClusterId] =
      _.span(s"$prefix.insert")

    def fetchAll(): Mid[DB, List[Cluster]] =
      _.span(s"$prefix.fetchAll")

    def fetchById(id: ClusterId): Mid[DB, Option[Cluster]] =
      _.span(s"$prefix.fetchById")
  }

  final class Impl(sql: ClusterSQL) extends ClusterRepo[ConnectionIO] {
    import io.conduktor.util.implicits.coercible._
    import doobie.postgres.implicits._

    def insert(params: CreateClusterParams): ConnectionIO[ClusterId] =
      sql.insert(params).withUniqueGeneratedKeys[ClusterId]("id")

    def fetchAll(): ConnectionIO[List[Cluster]] =
      sql.select().to[List]

    def fetchById(id: ClusterId): ConnectionIO[Option[Cluster]] =
      sql.selectById(id).option
  }
}
