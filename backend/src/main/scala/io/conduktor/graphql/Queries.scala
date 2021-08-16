package io.conduktor.graphql

import caliban.Value.StringValue
import io.conduktor.graphql.model.cluster.Cluster
import io.conduktor.graphql.model.topic.Topic
import io.conduktor.repo.model.cluster.Cluster.ClusterId
import io.conduktor.service.ServiceModule
import io.conduktor.service.topic.TopicService
import tofu.syntax.error._
import cats.syntax.show._

import scala.annotation.unused

trait Queries[F[_]] {
  def fetchClusterById(id: ClusterId): F[Option[Cluster]]

  def unsafeFetchClusterById(id: ClusterId): F[Cluster]

  def clusters: F[List[Cluster]]

  def fetchTopicByClusterId(id: ClusterId): F[List[Topic]]
}

object Queries {
  def apply(serviceModule: ServiceModule[GraphQLEffect]): Queries[GraphQLEffect] = {
    val impl: Queries[GraphQLEffect] = new Impl(serviceModule)
    impl
  }

  final class Impl(@unused serviceModule: ServiceModule[GraphQLEffect]) extends Queries[GraphQLEffect] {

    import zio.interop.catz.core._

    def clusters: GraphQLEffect[List[Cluster]] = for {
      result <- serviceModule.cluster.fetchAll()
    } yield result.map(db => Cluster(db, fetchTopicByClusterId(db.id)))

    def fetchClusterById(id: ClusterId): GraphQLEffect[Option[Cluster]] = for {
      result <- serviceModule.cluster.fetchById(id)
    } yield result.map(db => Cluster(db, fetchTopicByClusterId(db.id)))

    def unsafeFetchClusterById(id: ClusterId): GraphQLEffect[Cluster] =
      for {
        db <- serviceModule.cluster.unsafeFetchById(id)
      } yield Cluster(db, fetchTopicByClusterId(db.id))

    def fetchTopicByClusterId(id: ClusterId): GraphQLEffect[List[Topic]] = (for {
      result <- serviceModule.topic.fetchByClusterId(id)
    } yield result.map(entry => Topic(entry))).adaptError[Throwable] {
      case ex @ TopicService.Errors.ClusterNotFound(id) =>
        GraphqlError(
          "NOT_FOUND",
          ex.getMessage,
          Map(
            "clusterId" -> StringValue(id.show)
          )
        )
    }
  }
}
