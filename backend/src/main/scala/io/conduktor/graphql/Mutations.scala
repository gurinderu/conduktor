package io.conduktor.graphql

import derevo.derive
import io.conduktor.graphql.model.cluster.{CreateClusterPayload, CreateParams}
import io.conduktor.service
import io.conduktor.service.ServiceModule
import io.conduktor.util.implicits.chimney._
import tofu.higherKind.derived.representableK
import io.scalaland.chimney.dsl._

@derive(representableK)
trait Mutations[F[_]] {
  def createCluster(params: model.cluster.CreateParams): F[CreateClusterPayload]
}

object Mutations {
  def apply(
    serviceModule: ServiceModule[GraphQLEffect],
    queries: Queries[GraphQLEffect]
  ): Mutations[GraphQLEffect] = {
    val res = new Impl(serviceModule, queries)
    res
  }

  final class Impl(serviceModule: ServiceModule[GraphQLEffect], queries: Queries[GraphQLEffect])
      extends Mutations[GraphQLEffect] {
    def createCluster(params: CreateParams): GraphQLEffect[CreateClusterPayload] =
      for {
        id <- serviceModule.cluster.create(
          params
            .into[service.cluster.model.CreateClusterParams]
            .withFieldComputed(
              _.properties,
              _.properties.map(_.toList.map(p => p.key -> p.value).toMap).getOrElse(Map.empty)
            )
            .transform
        )
      } yield CreateClusterPayload(id, queries.unsafeFetchClusterById(id))
  }
}
