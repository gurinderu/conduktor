package io.conduktor.graphql

import caliban.GraphQL.graphQL
import caliban.wrappers.{ApolloCaching, ApolloTracing, Wrapper, Wrappers}
import caliban.{GraphQL, RootResolver}
import cats.Monad
import cats.arrow.FunctionK
import cats.effect.{Blocker, Sync, Timer}
import eu.timepit.refined.auto._
import fs2.Stream
import io.conduktor.config.AppConfig
import io.conduktor.service.ServiceModule
import io.conduktor.streaming.StreamingModule
import io.conduktor.util.implicits.graphql._
import io.conduktor.util.implicits.lift._
import io.conduktor.util.syntax.graphql._
import io.conduktor.util.syntax.http4s._
import io.conduktor.{IsoTask, RunServiceCtx}
import org.http4s.HttpRoutes
import tofu.common.TimeZone
import tofu.lift.Lift
import tofu.logging.{LoggingCompanion, Logs}
import tofu.syntax.lift._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import zio._
import zio.duration.{DurationOps, Duration => ZDuration}

final case class GraphQLModule[F[_]](httpRoutes: HttpRoutes[F], wsRoutes: HttpRoutes[F])

object GraphQLModule extends LoggingCompanion[GraphQLModule] {

  implicit def streamLift[F[_], G[_]](implicit L: Lift[F, G]): Lift[Stream[F, *], Stream[G, *]] =
    Lift.byFunK(new FunctionK[Stream[F, *], Stream[G, *]] {
      def apply[A](fa: Stream[F, A]): Stream[G, A] = fa.translate(L.liftF)
    })

  import Mutation._

  def makeEffectIn[
    I[_]: Sync: Timer: TimeZone: IsoTask,
    F[_]: Monad: Logs[I, *[_]]
  ](
    serviceModule: ServiceModule[F],
    streamingModule: StreamingModule[fs2.Stream[F, *]],
    config: AppConfig,
    blocker: Blocker
  )(implicit
    WR: RunServiceCtx[F, I]
  ): I[GraphQLModule[F]] = {

    val service              = serviceModule.lift[GraphQLEffect]
    val queries              = Queries(service)
    val queryResult          = Query(queries)
    implicit val querySchema = queryResult.schema
    val mutations            = Mutations(service, queries)
    val mutation             = Mutation(mutations)
    val subscriptions        = Subscriptions(streamingModule.lift[Stream[GraphQLEffect, *]])
    val subscription         = Subscription(subscriptions)
    val graphql = graphQL(
      RootResolver(
        queryResolver = queryResult.query,
        mutationResolver = mutation,
        subscriptionResolver = subscription
      )
    )

    for {
      implicit0(log: Log[F]) <- Logs[I, F].service[GraphQLModule[Any]]
      wrappers: GraphQL[GraphQLCtx] =
        graphql @@
          Wrappers.maxDepth(config.graphql.maxDepth.value) @@
          Wrappers.maxFields(config.graphql.maxFields.value) @@
          Wrappers.timeout(ZDuration.fromScala(config.graphql.timeout)) @@
          slowQueryWrapper(ZDuration.fromScala(config.graphql.slow)) @@
          ApolloTracing.apolloTracing @@
          ApolloCaching.apolloCaching
      interpreter <-
        (wrappers.interpreter.map(GraphqlError.withErrorCodeExtensions): Task[GraphQLCtxInterpreter]).lift[I]
      httpRoutes = interpreter.asHttpService(blocker, "/tmp").ilift[F]
      wsRoutes   = interpreter.asWsService.ilift[F]
    } yield GraphQLModule(httpRoutes, wsRoutes)
  }

  def slowQueryWrapper[F[_]: Log: Lift[*[_], GraphQLEffect]](slow: ZDuration): Wrapper.OverallWrapper[GraphQLCtx] =
    Wrappers.onSlowQueries(slow) { case (time, query) =>
      warn"Slow query took ${time.render}:\n$query".lift[GraphQLEffect].ignore
    }
}
