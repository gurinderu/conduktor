package io.conduktor.database

import io.conduktor.config.DbConfig
import io.conduktor.executors.ExecutionContexts
import io.conduktor.util.syntax.resource._
import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import cats.syntax.option._
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory
import doobie.Transactor
import doobie.hikari.HikariTransactor
import io.chrisdavenport.epimetheus.CollectorRegistry
import io.chrisdavenport.epimetheus.CollectorRegistry.Unsafe

object DoobieTransactor {
  final private val defaultConnectECThreadCount = 10
  final private val maxConnectECThreadCount     = 32

  def makeResource[I[_]: Async: ContextShift](
    dbConfig: DbConfig,
    cr: CollectorRegistry[I]
  ): Resource[I, Transactor[I]] = {
    import dbConfig._
    import dbConfig.connection._

    val ceSize = hikari
      .flatMap(_.maximumPoolSize)
      .map(_.value)
      .getOrElse(defaultConnectECThreadCount)
      .min(maxConnectECThreadCount)
    val ceTf = ExecutionContexts.namedThreadFactory("hikari-get-connection")
    val teTf = ExecutionContexts.namedThreadFactory("hikari-operation")
    for {
      ce <- ExecutionContexts.fixedThreadPool[I](ceSize, ceTf.some)
      te <- ExecutionContexts.cachedThreadPool[I](teTf.some)
      teBlocker = Blocker.liftExecutionContext(te)
      xa <- HikariTransactor.newHikariTransactor(
        driverClassName = driverClassName.value,
        url = url.value,
        user = username.value,
        pass = new String(password.value),
        connectEC = ce,
        blocker = teBlocker
      )
      _ <- xa.configure { ds =>
        Sync[I].delay {
          hikari.foreach { h =>
            h.maximumPoolSize.map(_.value).foreach(ds.setMaximumPoolSize)
            h.maxLifetime.foreach(mlt => ds.setMaxLifetime(mlt.toMillis))
            h.connectionTimeout.foreach(ct => ds.setConnectionTimeout(ct.toMillis))
            h.registerMbeans.foreach(v => ds.setRegisterMbeans(v))
            ds.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(Unsafe.asJava(cr)))
          }
        }
      }.toResource
    } yield xa
  }

}
