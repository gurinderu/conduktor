package io.conduktor.http

import cats.effect.{Blocker, Clock, Concurrent, ConcurrentEffect, ContextShift, Resource, Sync, Timer}
import cats.syntax.option._
import cats.syntax.semigroupk._
import cats.syntax.show._
import cats.{Endo, Monad}
import derevo.derive
import fs2.Stream
import io.chrisdavenport.epimetheus.CollectorRegistry
import io.chrisdavenport.epimetheus.CollectorRegistry.Unsafe
import io.conduktor.context.ServiceCtx
import io.conduktor.executors.ExecutionContexts
import io.conduktor.graphql.GraphQLModule
import io.conduktor.util.syntax.resource._
import io.conduktor.util.tracing.shims._
import io.conduktor.{LogsSame, RunServiceCtx}
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.http4s.common.Request_
import io.janstenpickle.trace4cats.http4s.server.syntax._
import io.janstenpickle.trace4cats.inject.{EntryPoint, Trace}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.metrics.prometheus.{Prometheus, PrometheusExportService}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig, Metrics, RequestId, RequestLogger}
import org.http4s.util.CaseInsensitiveString
import tofu.generate.GenUUID
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, LoggingCompanion, Logs}
import tofu.syntax.monadic._
import tofu.{BracketThrow, MonadThrow, WithProvide}

import java.net.URI
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.Try

@derive(representableK)
trait HttpServer[S[_]] {
  def serve: S[Unit]
}

object HttpServer extends LoggingCompanion[HttpServer] {

  object URIVar extends {
    def unapply(str: String): Option[URI] =
      if (str.isEmpty || str.contains("../")) None
      else Try(URI.create(str)).toOption
  }

  def makeResource[
    //@formatter:off
    I[_] : ConcurrentEffect : Timer : LogsSame,
    F[_] : Sync : ContextShift : RunServiceCtx[*[_], I] : Clock : Trace : Logs[I, *[_]],
    //@formatter:on
  ](
    graphql: GraphQLModule[F],
    entryPoint: EntryPoint[I],
    cr: CollectorRegistry[I],
    platformEC: ExecutionContext
  )(implicit WP: WithProvide[F, I, ServiceCtx[I]]): Resource[I, HttpServer[Stream[I, *]]] = {

    object dsl extends Http4sDsl[F]
    import dsl._

    implicit val uuidDecoder: QueryParamDecoder[UUID] = QueryParamDecoder[String].map(UUID.fromString)

    def playgroundRoute(blocker: Blocker): HttpRoutes[F] =
      HttpRoutes.of[F] { case GET -> Root / "playground.html" =>
        StaticFile
          .fromResource[F]("/assets/playground.html", blocker)
          .getOrElseF(NotFound())
      }

    import scala.concurrent.duration._

    val cosrConfug = CORSConfig(
      anyOrigin = true,
      anyMethod = false,
      allowedMethods = Some(Set("GET", "POST")),
      allowCredentials = true,
      maxAge = 1.day.toSeconds)

    for {
      implicit0(logI: Log[I])     <- Logs[I, I].service[HttpServer[Any]].toResource
      implicit0(logF: Logging[F]) <- Logs[I, F].forService[HttpServer[Any]].toResource
      blocker                     <- ExecutionContexts.blocker[I]("http4s-blocker")

      ctxRoutes = Router[F](
        "/api/graphql" -> graphql.httpRoutes,
        "/ws/graphql"  -> graphql.wsRoutes,
        "/" -> playgroundRoute(
          blocker
        )
      )
      metered <- mw.mkMetered(cr)
      routes = CORS(metered(mw.withRequestId(mw.logged(mw.traced(ctxRoutes, entryPoint)))), cosrConfug)
      _ <-
        BlazeServerBuilder[I](platformEC)
          .bindHttp(8080, "0.0.0.0")
          .withoutBanner
          .withHttpApp(routes.orNotFound)
          .resource
      server = new HttpServer[Stream[I, *]] {
        def serve: Stream[I, Unit] = Stream.never
      }
    } yield server
  }

  //route middlewares
  object mw {
    def mkMetered[I[_]: Sync: Clock](cr: CollectorRegistry[I]): Resource[I, Endo[HttpRoutes[I]]] = {
      val jcr = Unsafe.asJava(cr)
      for {
        metricsOps <- Prometheus.metricsOps[I](jcr, "server")
      } yield (routes: HttpRoutes[I]) => Metrics[I](metricsOps)(routes) <+> PrometheusExportService.service[I](jcr)
    }

    def withRequestId[I[_]: Sync: GenUUID](routes: HttpRoutes[I]): HttpRoutes[I] =
      RequestId.httpRoutes[I](genReqId = GenUUID.random[I])(routes)

    def traced[F[_]: RunServiceCtx[*[_], I]: Monad: Trace, I[_]: BracketThrow](
      routes: HttpRoutes[F],
      ep: EntryPoint[I]
    ): HttpRoutes[I] =
      routes.injectContext(ep, makeContext = mkServiceCtx[I])

    private def mkServiceCtx[I[_]: MonadThrow](req: Request_, span: Span[I]): I[ServiceCtx[I]] =
      extractRequestId(req).map(reqId => ServiceCtx(reqId, span))

    private val XRequestID: CaseInsensitiveString = "X-Request-ID".ci

    private def extractRequestId[F[_]](req: Request_)(implicit F: MonadThrow[F]): F[String] =
      req.headers
        .get(XRequestID)
        .liftTo(MissingHeader(XRequestID.value))
        .map(_.value)

    final case class MissingHeader(header: String) extends MessageFailure {
      def message: String = show"Missing `$header` header"

      def cause: Option[Throwable] = none

      def toHttpResponse[F[_]](httpVersion: HttpVersion): Response[F] =
        Response(Status.BadRequest, httpVersion)
          .withEntity(message)
    }

    def logged[I[_]: Concurrent](routes: HttpRoutes[I])(implicit L: Log[I]): HttpRoutes[I] =
      RequestLogger.httpRoutes[I](logHeaders = true, logBody = false, logAction = Some(L.debug(_)))(routes)
  }
}
