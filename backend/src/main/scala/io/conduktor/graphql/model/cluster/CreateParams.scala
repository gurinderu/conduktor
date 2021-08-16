package io.conduktor.graphql.model.cluster

import caliban.schema.ArgBuilder
import cats.data.NonEmptyList
import eu.timepit.refined.types.string.{NonEmptyFiniteString}
import io.conduktor.graphql.GraphQLSchema

final case class CreateParams(
  name: NonEmptyFiniteString[255],
  bootstrapServers: NonEmptyFiniteString[255],
  properties: Option[NonEmptyList[Property]]
)

object CreateParams {

  import io.conduktor.graphql.implicits.generic._
  import io.conduktor.graphql.implicits.refined._
  import Property._

  implicit val createParamsSchema: GraphQLSchema[CreateParams]  = gen
  implicit val createParamsArgBuilder: ArgBuilder[CreateParams] = ArgBuilder.gen
}
