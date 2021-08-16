package io.conduktor.util.implicits

import caliban.CalibanError.ExecutionError
import caliban.schema.{ArgBuilder, Schema}
import caliban.uploads.Uploads
import cats.effect.Timer
import cats.syntax.either._
import cats.{Applicative, Defer, FlatMap}
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import io.conduktor.RunServiceCtx
import io.conduktor.context.ServiceCtx
import io.conduktor.graphql.GraphQLEffect
import io.conduktor.util.zclock
import io.estatico.newtype.Coercible
import tofu.common.TimeZone
import tofu.lift.IsoK
import tofu.syntax.lift._
import zio.random.Random
import zio.{RIO, Task, ZLayer}

object graphql {
  implicit def isoGraphQLEffectF[F[_], I[_]](implicit
    WR: RunServiceCtx[F, I],
    isoK: IsoK[I, Task],
    I1: Defer[I],
    I2: Applicative[I],
    I3: Timer[I],
    I4: TimeZone[I],
    F: FlatMap[F]
  ): IsoK[GraphQLEffect, F] =
    new IsoK[GraphQLEffect, F] {
      import zio.interop.catz.core._
      def to[A](fa: GraphQLEffect[A]): F[A] =
        WR.askF(r =>
          isoK
            .from(
              fa.provideLayer(
                ZLayer.succeed(r.lift[Task]) ++ ZLayer.succeed(zclock.make[I]) ++ Random.live ++ Uploads.empty
              )
            )
            .lift[F]
        )
      def from[A](ga: F[A]): GraphQLEffect[A] =
        RIO.fromFunctionM(r => isoK.tof(WR.runContext(ga)(r.get[ServiceCtx[Task]].mapK(isoK.fromF))))
    }

  trait RefinedSchema[R] {
    implicit def refined[T, P](implicit S: Schema[R, T]): Schema[R, Refined[T, P]] =
      S.contramap(_.value)

    implicit def refinedArgs[T, P](implicit V: Validate[T, P], A: ArgBuilder[T]): ArgBuilder[Refined[T, P]] =
      A.flatMap(value =>
        refineV[P](value).leftMap(str => ExecutionError(s"Can't build a parameter from input $value. Reason: $str"))
      )

  }

  trait CoercibleSchema[R] {
    implicit def coercibleSchema[T, F](implicit ev: Coercible[F, T], S: Schema[R, T]): Schema[R, F] =
      S.contramap(ev(_))

    implicit def coercibleArgBuilder[T, F](implicit ev: Coercible[T, F], A: ArgBuilder[T]): ArgBuilder[F] =
      A.map(ev(_))
  }
}
