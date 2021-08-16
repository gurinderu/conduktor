package io.conduktor.graphql.model.cluster

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyFiniteString
import io.conduktor.graphql.model.topic.Topic
import io.conduktor.graphql.{GraphQLEffect, GraphQLSchema}
import io.conduktor.repo
import io.conduktor.repo.model.cluster.Cluster.ClusterId

final case class Cluster(
  id: ClusterId,
  name: NonEmptyFiniteString[255],
  bootstrapServers: NonEmptyFiniteString[255],
  properties: Option[NonEmptyList[Property]],
  topics: GraphQLEffect[List[Topic]]
)

object Cluster {

  import io.conduktor.graphql.implicits.generic._
  import io.conduktor.graphql.implicits.refined._
  import io.conduktor.graphql.implicits.coercible._
  import Topic._
  import Property._

  def apply(db: repo.model.cluster.Cluster, topics: GraphQLEffect[List[Topic]]): Cluster = new Cluster(
    id = db.id,
    name = db.name,
    bootstrapServers = db.bootstrapServers,
    properties = NonEmptyList.fromList(db.properties.map { case (k, v) =>
      Property(k, v)
    }.toList),
    topics = topics
  )

  implicit val serverSchema: GraphQLSchema[Cluster] = gen
}
