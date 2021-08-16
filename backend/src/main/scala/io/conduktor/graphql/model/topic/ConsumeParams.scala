package io.conduktor.graphql.model.topic

import caliban.schema.ArgBuilder
import eu.timepit.refined.types.string.NonEmptyString
import io.conduktor.enums.{Format, Offset}
import io.conduktor.graphql.GraphQLSchema
import io.conduktor.repo.model.cluster.Cluster.ClusterId

final case class ConsumeParams(
  clusterId: ClusterId,
  topicName: NonEmptyString,
  keyFormat: Format,
  valueFormat: Format,
  offset: Offset
)

object ConsumeParams {

  import io.conduktor.graphql.implicits.generic._
  import io.conduktor.graphql.implicits.coercible._
  import io.conduktor.graphql.implicits.refined._

  implicit val consumeParamsSchema: GraphQLSchema[ConsumeParams]  = gen
  implicit val consumeParamsArgBuilder: ArgBuilder[ConsumeParams] = ArgBuilder.gen
}
