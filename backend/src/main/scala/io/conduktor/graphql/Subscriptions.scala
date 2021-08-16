package io.conduktor.graphql

import io.conduktor.graphql.model.topic.{ConsumeParams, ConsumeResult}
import io.conduktor.streaming
import io.conduktor.streaming.StreamingModule
import io.scalaland.chimney.dsl._

trait Subscriptions[S[_]] {
  def consumer(params: ConsumeParams): S[ConsumeResult]
}

object Subscriptions {

  import zio.stream.interop.fs2z._

  def apply(streamingModule: StreamingModule[fs2.Stream[GraphQLEffect, *]]): Subscriptions[GraphQLStream] = {
    val res = new Impl(streamingModule)
    res
  }

  final class Impl(streamingModule: StreamingModule[fs2.Stream[GraphQLEffect, *]])
      extends Subscriptions[GraphQLStream] {

    import io.conduktor.util.implicits.chimney._

    def consumer(params: ConsumeParams): GraphQLStream[ConsumeResult] = (for {
      result <- streamingModule.consumer
        .consume(params.into[streaming.consumer.model.ConsumeParams].transform)
    } yield result.into[ConsumeResult].transform).toZStream(16)
  }

}
