package io.conduktor.graphql.model.cluster

import io.conduktor.graphql.GraphQLSchema

final case class Property(key: String, value: String)

object Property {
  import io.conduktor.graphql.implicits.generic._

  implicit val propertySchema: GraphQLSchema[Property] = gen
}
