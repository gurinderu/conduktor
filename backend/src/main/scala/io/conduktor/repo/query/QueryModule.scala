package io.conduktor.repo.query

import cats.Id
import cats.data.Reader
import cats.effect.Effect
import cats.tagless.syntax.functorK._
import derevo.derive
import doobie.util.log.LogHandler
import tofu.WithContext
import tofu.higherKind.derived.representableK
import tofu.syntax.funk.funK

@derive(representableK)
trait QueryModule[F[_]] {
  def cluster: F[ClusterSQL]
}

object QueryModule {
  def makeWithContext[I[_]: Effect, F[_]: WithContext[*[_], LogHandler]](): QueryModule[F] =
    new PgQueryModule[F]

  def make[I[_]: Effect](): QueryModule[Id] =
    makeWithContext[I, Reader[LogHandler, *]]().mapK(funK(_.run(LogHandler.nop)))

  final class PgQueryModule[F[_]: WithContext[*[_], LogHandler]]() extends QueryModule[F] {
    def cluster: F[ClusterSQL] = ClusterSQL.makeWithContext[F]
  }

}
