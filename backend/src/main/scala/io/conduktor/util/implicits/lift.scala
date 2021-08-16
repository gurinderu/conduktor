package io.conduktor.util.implicits

import tofu.lift.{IsoK, Lift}

object lift extends LiftLowerInstances {
  implicit def lift1FromIsoK[F[_], G[_]](implicit isoK: IsoK[F, G]): Lift[F, G] =
    new Lift[F, G] {
      def lift[A](fa: F[A]): G[A] = isoK.to(fa)
    }
}

trait LiftLowerInstances {
  implicit def lift2FromIsoK[F[_], G[_]](implicit isoK: IsoK[F, G]): Lift[G, F] =
    new Lift[G, F] {
      def lift[A](ga: G[A]): F[A] = isoK.from(ga)
    }
}
