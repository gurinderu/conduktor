package io.conduktor.graphql.model.topic

import io.conduktor.graphql.GraphQLSchema

import java.time.Instant

final case class ConsumeResult(
  time: Instant,
  key: String,
  value: String
)

object ConsumeResult {

  import io.conduktor.graphql.implicits.generic._

  implicit val consumeResultSchema: GraphQLSchema[ConsumeResult] = gen
}
