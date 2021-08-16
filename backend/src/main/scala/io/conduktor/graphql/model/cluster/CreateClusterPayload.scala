package io.conduktor.graphql.model.cluster

import io.conduktor.graphql.{GraphQLEffect, GraphQLSchema}
import io.conduktor.repo.model.cluster.Cluster.ClusterId

final case class CreateClusterPayload(id: ClusterId, cluster: GraphQLEffect[Cluster])

object CreateClusterPayload {

  import io.conduktor.graphql.implicits.generic._
  import io.conduktor.graphql.implicits.coercible._
  import Cluster._

  implicit val createServerPayloadSchema: GraphQLSchema[CreateClusterPayload] = gen
}
