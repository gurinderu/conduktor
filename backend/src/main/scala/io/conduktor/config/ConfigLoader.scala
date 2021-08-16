package io.conduktor.config

import cats.Functor
import cats.effect.{ContextShift, Sync}
import derevo.derive
import io.conduktor.LogsSame
import pureconfig.generic.ProductHint
import pureconfig.module.catseffect2.syntax._
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource, KebabCase}
import tofu.BlockExec
import tofu.higherKind.derived.representableK
import tofu.logging.{LoggingCompanion, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.scoped._

import scala.annotation.unused

@derive(representableK)
trait ConfigLoader[F[_]] {
  def loadConfig(namespace: String): F[AppConfig]
}

object ConfigLoader extends LoggingCompanion[ConfigLoader] {

  @inline def makeEffect[I[_]: Sync: ContextShift: BlockExec: LogsSame]: I[ConfigLoader[I]] =
    makeEffectIn[I, I]

  def makeEffectIn[I[_]: Functor, F[_]: Sync: ContextShift: BlockExec: Logs[I, *[_]]]: I[ConfigLoader[F]] =
    for {
      implicit0(log: Log[F]) <- Logs[I, F].service[ConfigLoader[Any]]
    } yield new Impl[F]

  final class Impl[F[_]: Sync: ContextShift: BlockExec: Log] extends ConfigLoader[F] {
    def loadConfig(namespace: String): F[AppConfig] = {
      @unused implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, KebabCase))

      for {
        _   <- info"Loading Typesafe config from default source at '$namespace' namespace"
        cfg <- withBlocker(ConfigSource.default.at(namespace).loadF[F, AppConfig](_))
        _   <- info"Loaded config: $cfg"
      } yield cfg
    }
  }
}
