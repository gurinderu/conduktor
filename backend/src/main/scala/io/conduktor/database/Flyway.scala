package io.conduktor.database

import io.conduktor.LogsSame
import io.conduktor.config.DbConfig
import cats.effect.Sync
import cats.instances.function._
import cats.instances.option._
import cats.syntax.foldable._
import cats.tagless.syntax.functorK._
import cats.{Applicative, Apply, Endo, Monoid, MonoidK}
import derevo.derive
import mouse.any._
import mouse.option._
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.{Flyway => JFlyway}
import tofu.Blocks
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{LoggingCompanion, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait Flyway[F[_]] {
  def clean: F[Unit]
  def migrate: F[Unit]
  def refresh: F[Unit]
}

object Flyway extends LoggingCompanion[Flyway] {
  @inline def makeEffect[I[_]: Sync: Blocks: LogsSame](dbConfig: DbConfig): I[Flyway[I]] =
    makeEffectIn[I, I](dbConfig)

  def makeEffectIn[I[_]: Sync, F[_]: Sync: Blocks: Logs[I, *[_]]](
    dbConfig: DbConfig
  ): I[Flyway[F]] =
    if (dbConfig.flyway.cata(_.migrationsEnabled, false))
      for {
        implicit0(log: Log[F]) <- Logs[I, F].service[Flyway[Any]]
        jFlyway                <- Sync[I].delay(JFlyway.configure() |> configureWith(dbConfig) |> (_.load()))
        mid  = new LoggingMid[F]
        impl = (new Impl[F](jFlyway): Flyway[F]).mapK(Blocks[F].funK)
      } yield mid attach impl
    else
      new Noop[F].pure[I].widen

  private def configureWith(dbConfig: DbConfig): Endo[FluentConfiguration] = {
    implicit def endoMonoid[A]: Monoid[Endo[A]] = MonoidK[Endo].algebra[A]
    _.dataSource(
      dbConfig.connection.url.value,
      dbConfig.connection.username.value,
      new String(dbConfig.connection.password.value)
    )
      .validateMigrationNaming(true) |>
      dbConfig.flyway.foldMap { flywayConfig =>
        _.locations(flywayConfig.locations: _*)
          .cleanDisabled(flywayConfig.cleanDisabled)
      }
  }

  final class Impl[F[_]](jFlyway: JFlyway)(implicit F: Sync[F]) extends Flyway[F] {
    def clean: F[Unit]   = F.delay(jFlyway.clean()).void
    def migrate: F[Unit] = F.delay(jFlyway.migrate()).void
    def refresh: F[Unit] = clean *> migrate
  }

  final class LoggingMid[F[_]: Apply: Log] extends Flyway[Mid[F, *]] {
    def clean: Mid[F, Unit]   = info"Start database cleanup" *> _ <* info"Database cleanup finished"
    def migrate: Mid[F, Unit] = info"Start database migration" *> _ <* info"Database migration finished"
    def refresh: Mid[F, Unit] = info"Start database refresh" *> _ <* info"Database refresh finished"
  }

  final class Noop[F[_]: Applicative] extends Flyway[F] {
    def clean: F[Unit]   = unit[F]
    def migrate: F[Unit] = unit[F]
    def refresh: F[Unit] = unit[F]
  }
}
