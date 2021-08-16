package io.conduktor.util.syntax

import cats.data.Kleisli
import cats.{~>, Functor}
import org.http4s.HttpRoutes
import tofu.WithProvide
import tofu.lift.IsoK

object http4s {
  implicit class HttpRoutesOps[F[_]](private val self: HttpRoutes[F]) extends AnyVal {
    def imapK[G[_]: Functor](fk: F ~> G)(gk: G ~> F): HttpRoutes[G] =
      Kleisli(req => self.run(req.mapK(gk)).mapK(fk).map(_.mapK(fk)))

    def ilift[G[_]](implicit isoK: IsoK[F, G], G: Functor[G]): HttpRoutes[G] =
      imapK(isoK.tof)(isoK.fromF)

    def runContext[G[_], Ctx](ctx: Ctx)(implicit WP: WithProvide[F, G, Ctx], G: Functor[G]): HttpRoutes[G] =
      self.imapK[G](WP.runContextK(ctx))(WP.liftF)
  }
}
