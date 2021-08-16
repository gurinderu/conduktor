package io.conduktor.graphql.model.cluster

import caliban.schema.ArgBuilder
import io.conduktor.graphql.GraphQLSchema
import io.conduktor.repo.model.cluster.Cluster.ClusterId

final case class FetchParams(id: ClusterId)

object FetchParams {
  import io.conduktor.graphql.implicits.generic._
  import io.conduktor.graphql.implicits.coercible._

  implicit val fetchParamsSchema: GraphQLSchema[FetchParams]  = gen
  implicit val fetchParamsArgBuilder: ArgBuilder[FetchParams] = ArgBuilder.gen
}
