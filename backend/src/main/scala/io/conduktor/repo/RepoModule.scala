package io.conduktor.repo

import cats.Monad
import cats.data.Reader
import cats.effect.Effect
import doobie.LogHandler
import io.conduktor.repo.query.QueryModule
import io.janstenpickle.trace4cats.inject.Trace
import tofu.doobie.LiftConnectionIO
import tofu.lift.UnliftIO
import tofu.logging.Logs
import tofu.syntax.monadic._

trait RepoModule[DB[_]] {
  def cluster: ClusterRepo[DB]
}

object RepoModule {
  def makeEffectIn[
    I[_]: Effect,
    DB[_]: Monad: UnliftIO: Trace: LiftConnectionIO: Logs[I, *[_]]
  ](): I[RepoModule[DB]] = {
    type G[x] = Reader[LogHandler, x]
    for {
      _ <- unit[I]
      qm = QueryModule.makeWithContext[I, G]()
      cluster <- ClusterRepo.makeEffectIn[I, G, DB](qm.cluster)
    } yield Impl(cluster)
  }

  final case class Impl[DB[_]](cluster: ClusterRepo[DB]) extends RepoModule[DB]
}
