package io.conduktor.repo.query

import doobie.{Query0, Update0}
import doobie.implicits._
import doobie.util.log.LogHandler
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import io.conduktor.repo.model.cluster.{Cluster, CreateClusterParams}
import tofu.WithContext
import tofu.syntax.context.ask
import io.conduktor.util.implicits.coercible._

trait ClusterSQL {
  def selectById(id: ClusterId): Query0[Cluster]

  def select(): Query0[Cluster]

  def insert(params: CreateClusterParams): Update0
}

object ClusterSQL {

  def makeWithContext[F[_]: WithContext[*[_], LogHandler]]: F[ClusterSQL] =
    ask[F] { implicit lh: LogHandler =>
      new PgClusterSQL
    }

  final class PgClusterSQL(implicit lh: LogHandler) extends ClusterSQL {

    import doobie.postgres.implicits._
    import doobie.refined.implicits._

    def insert(params: CreateClusterParams): Update0 =
      sql"""
           |INSERT INTO cluster(
           |  name,
           |  bootstrap_servers,
           |  properties) VALUES(
           |  ${params.name},
           |  ${params.bootstrapServers},
           |  ${params.properties}
           |  )
           |""".stripMargin.update

    def select(): Query0[Cluster] =
      sql"""
           |SELECT
           |  id,
           |  name,
           |  bootstrap_servers,
           |  properties
           |FROM
           |  cluster
         """.stripMargin.query[Cluster]

    def selectById(id: ClusterId): Query0[Cluster] =
      sql"""
           |SELECT
           |  id,
           |  name,
           |  bootstrap_servers,
           |  properties
           |FROM
           |  cluster
           |WHERE
           |  id = $id
         """.stripMargin.query[Cluster]
  }
}
