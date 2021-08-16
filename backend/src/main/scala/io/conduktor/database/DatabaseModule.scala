package io.conduktor.database

import cats.Defer
import cats.effect.{Async, BracketThrow, ContextShift, Resource}
import io.chrisdavenport.epimetheus.CollectorRegistry
import io.conduktor.config.DbConfig
import tofu.doobie.transactor.Txr
import tofu.lift.Lift

final case class DatabaseModule[F[_]](
  txr: Txr.Continuational[F]
)

object DatabaseModule {
  def makeResourceIn[I[_]: Async: ContextShift, F[_]: BracketThrow: Defer: Lift[I, *[_]]](
    dbConfig: DbConfig,
    collectorRegistry: CollectorRegistry[I]
  ): Resource[I, DatabaseModule[F]] =
    for {
      t <- DoobieTransactor.makeResource[I](dbConfig, collectorRegistry)
      txr = Txr.continuational(t.mapK(Lift.trans[I, F]))
    } yield DatabaseModule(txr)
}
