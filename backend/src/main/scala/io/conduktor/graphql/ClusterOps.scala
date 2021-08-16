package io.conduktor.graphql

import io.conduktor.graphql.model.cluster.{CreateClusterPayload, CreateParams}

final case class ClusterOps(
  add: CreateParams => GraphQLEffect[CreateClusterPayload]
)

object ClusterOps {

  import io.conduktor.graphql.implicits.generic._
  import CreateParams._
  import CreateClusterPayload._

  implicit val serverOpsSchema: GraphQLSchema[ClusterOps] = gen
}
