package io.conduktor.graphql

import io.conduktor.graphql.model.cluster.{Cluster, FetchParams}

import scala.annotation.unused

final case class Query(
  clusters: GraphQLEffect[List[Cluster]],
  clusterById: FetchParams => GraphQLEffect[Option[Cluster]]
)

object Query {
  final case class QueryResult(query: Query, schema: GraphQLSchema[Query])

  import io.conduktor.graphql.implicits.generic._

  def apply(@unused res: Queries[GraphQLEffect]): QueryResult = {
    val schema: GraphQLSchema[Query] = gen

    QueryResult(
      new Query(
        res.clusters,
        p => res.fetchClusterById(p.id)
      ),
      schema
    )
  }

}
