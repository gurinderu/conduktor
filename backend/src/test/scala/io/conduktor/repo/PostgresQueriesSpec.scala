package io.conduktor.repo

import cats.Id
import cats.effect.{Blocker, ContextShift, IO, Resource}
import cats.syntax.option._
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import io.conduktor.config.DbConfig
import io.conduktor.database.Flyway
import io.conduktor.executors.ExecutionContexts
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import io.conduktor.repo.model.cluster.CreateClusterParams
import io.conduktor.repo.query.QueryModule
import io.conduktor.util.syntax.resource._
import io.conduktor.util.types._
import org.scalatest.Assertions
import org.scalatest.flatspec.AnyFlatSpec
import tofu.logging.Logs
import tofu.syntax.monadic._

import java.util.UUID

class PostgresQueriesSpec extends AnyFlatSpec with TestContainerForAll {
  override val containerDef                   = PostgreSQLContainer.Def().copy()
  implicit val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)
  it should "have valid queries" in withContainers { container =>
    val transactor = Transactor.fromDriverManager[IO](
      container.driverClassName,
      url = container.jdbcUrl,
      user = container.username,
      pass = container.password
    )

    import doobie.implicits._

    val f = for {
      _ <- unit[Resource[IO, *]]
      implicit0(logs: Logs[IO, IO]) = Logs.sync[IO, IO]
      _    <- sql"""CREATE EXTENSION IF NOT EXISTS "uuid-ossp"""".update.run.transact(transactor).toResource
      _    <- sql"""CREATE EXTENSION IF NOT EXISTS hstore""".update.run.transact(transactor).toResource
      pool <- ExecutionContexts.fixedThreadPool[IO](1)
      implicit0(blocker: Blocker) = Blocker.liftExecutionContext(pool)
      flyway <-
        Flyway
          .makeEffect[IO](
            DbConfig(
              connection = DbConfig.Connection(
                url = NonEmptyString.unsafeFrom(container.jdbcUrl),
                username = NonEmptyString.unsafeFrom(container.username),
                password = Secret.unsafeFrom(container.password)
              ),
              driverClassName = NonEmptyString.unsafeFrom("org.postgresql.Driver"),
              hikari = None,
              flyway = DbConfig.Flyway(migrationsEnabled = true, cleanDisabled = false).some
            )
          )
          .toResource
    } yield flyway

    f.use { case flyway =>
      flyway.migrate.map { _ =>
        val querySpec = new QuerySpec(transactor, QueryModule.make[IO]())
        querySpec.check()
      }
    }.unsafeRunSync()
  }
}

class QuerySpec(val transactor: Transactor[IO], qm: QueryModule[Id]) extends Assertions with IOChecker {
  def check(): Unit = {
    check(
      qm.cluster.insert(
        CreateClusterParams(
          "name",
          "127.0.0.1:9082",
          Map.empty
        )
      )
    )
    check(qm.cluster.select())
    check(qm.cluster.selectById(ClusterId(UUID.randomUUID())))

  }
}
