package io.conduktor.graphql.model.topic

import io.conduktor.graphql.GraphQLSchema
import io.conduktor.service

final case class Topic(name: String)

object Topic {

  import io.conduktor.graphql.implicits.generic._

  def apply(entry: service.topic.model.Topic): Topic =
    new Topic(entry.name)

  implicit val topicSchema: GraphQLSchema[Topic] = gen
}
