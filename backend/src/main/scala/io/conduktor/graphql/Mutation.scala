package io.conduktor.graphql

import scala.annotation.unused

final case class Mutation(
  cluster: ClusterOps
)

object Mutation {

  import io.conduktor.graphql.implicits.generic._
  import ClusterOps._

  implicit val mutationSchema: GraphQLSchema[Mutation] = gen

  def apply(@unused res: Mutations[GraphQLEffect]): Mutation = Mutation(
    ClusterOps(
      res.createCluster
    )
  )
}
