package io.conduktor

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Sync, Timer}
import fs2.Stream
import io.conduktor.AppF.{InitResult, StreamingApp}
import io.conduktor.config.{AppConfig, ConfigLoader}
import io.conduktor.context.{LogCtx, ServiceCtx}
import io.conduktor.database.{DatabaseModule, Flyway}
import io.conduktor.executors.ExecutionContexts
import io.conduktor.graphql.GraphQLModule
import io.conduktor.http.HttpServer
import io.conduktor.metrics.MetricsModule
import io.conduktor.repo.RepoModule
import io.conduktor.service.ServiceModule
import io.conduktor.streaming.StreamingModule
import io.conduktor.tracing.TracingModule
import io.conduktor.util.syntax.resource._
import io.janstenpickle.trace4cats.inject.Trace
import tofu.common.TimeZone
import tofu.doobie.transactor.Txr
import tofu.lift.{Lift, UnliftIO}
import tofu.logging.{LoggableContext, Logs}
import tofu.optics.Extract
import tofu.syntax.lift._
import tofu.syntax.monadic._
import tofu.{Execute, Fire, WithContext}

import scala.annotation.nowarn

trait AppF {
  //@formatter:off
  def runF[
    I[_] : ConcurrentEffect : ContextShift : Timer : TimeZone : Execute : IsoTask,
    F[_] : Concurrent :ContextShift: Timer : Fire : UnliftIO : RunServiceCtx[*[_], I]]: I[Unit] = {
    //@formatter:on
    implicit val logsI: LogsSame[I] = Logs.sync[I, I]

    // Reads configs and runs database migration, encapsulates resources needed at startup only
    @nowarn("cat=unused-params")
    def makeInitResource: Resource[I, InitResult] = {
      val blockerResource = ExecutionContexts.blocker[I]("init-blocker")

      blockerResource.evalMap { implicit blocker =>
        for {
          configLoader <- ConfigLoader.makeEffect[I]
          cfg          <- configLoader.loadConfig("app")
          flyway       <- Flyway.makeEffect[I](cfg.database)
          _            <- flyway.migrate
        } yield InitResult(cfg)
      }
    }

    val r = for {
      InitResult(config) <- makeInitResource.run.toResource
      MetricsModule(cr)  <- MetricsModule.makeEffect[I].toResource
      implicit0(txr: Txr.Continuational[F]) <- DatabaseModule
        .makeResourceIn[I, F](config.database, cr)
        .map(_.txr)

      implicit0(liftIDB: Lift[I, txr.DB]) = Lift.byFunK(Lift.trans[I, F] andThen Lift.trans[F, txr.DB])

      tracingModule <- TracingModule.makeResourceIn[I, F, txr.DB](config.tracing)
      implicit0(tracingF: Trace[F])       = tracingModule.tracingF
      implicit0(tracingDB: Trace[txr.DB]) = tracingModule.tracingDB

      implicit0(logsF: Logs[I, F])       = makeCtxLogs[I, F, ServiceCtx[I]]
      implicit0(logsDB: Logs[I, txr.DB]) = logsF.lift[txr.DB]

      repoModule      <- RepoModule.makeEffectIn[I, txr.DB]().toResource
      serviceModule   <- ServiceModule.makeResourceIn[I, F, txr.DB](repoModule)
      streamingModule <- StreamingModule.makeEffectIn[I, F, txr.DB](repoModule).toResource

      blocker       <- ExecutionContexts.blocker[I]("graphql-http4s-blocker")
      graphqlModule <- GraphQLModule.makeEffectIn[I, F](serviceModule, streamingModule, config, blocker).toResource

      patformEC <- Execute[I].executionContext.toResource
      httpServer <- HttpServer.makeResource[I, F](
        graphqlModule,
        tracingModule.entryPoint,
        cr,
        patformEC
      )

    } yield StreamingApp(httpServer)

    r.use(_.run)
  }

  def makeCtxLogs[I[_]: Sync, F[_]: Sync: *[_] WithContext Ctx, Ctx: * Extract LogCtx]: Logs[I, F] = {
    val logLens                                        = Extract[Ctx, LogCtx]
    implicit val gHasLocalLogCtx: F WithContext LogCtx = WithContext[F, Ctx].extract(logLens)
    implicit val loggableContextG: LoggableContext[F]  = LoggableContext.of[F].instance[LogCtx]
    Logs.withContext[I, F]
  }
}

object AppF {
  final case class InitResult(config: AppConfig)

  final case class StreamingApp[F[_]](httpServer: HttpServer[Stream[F, *]]) {
    def run(implicit F: Concurrent[F]): F[Unit] =
      Stream(httpServer.serve).parJoinUnbounded.compile.drain
  }
}
