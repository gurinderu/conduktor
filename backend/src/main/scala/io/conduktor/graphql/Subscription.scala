package io.conduktor.graphql

import io.conduktor.graphql.model.topic.{ConsumeParams, ConsumeResult}

final case class Subscription(
  consumer: ConsumeParams => GraphQLStream[ConsumeResult]
)

object Subscription {

  def apply(subscriptions: Subscriptions[GraphQLStream]) = new Subscription(
    consumer = subscriptions.consumer
  )

  import io.conduktor.graphql.implicits.generic._
  import ConsumeParams._
  import ConsumeResult._

  implicit val schema: GraphQLSchema[Subscription] = gen
}
