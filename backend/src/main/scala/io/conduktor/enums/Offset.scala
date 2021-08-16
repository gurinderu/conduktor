package io.conduktor.enums

import caliban.schema.ArgBuilder
import enumeratum.{CatsEnum, CirceEnum, DoobieEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.UpperSnakecase
import io.conduktor.graphql.GraphQLSchema
import io.conduktor.util.logging.LoggableEnum

sealed trait Offset extends EnumEntry with UpperSnakecase

object Offset
    extends Enum[Offset]
    with CatsEnum[Offset]
    with CirceEnum[Offset]
    with DoobieEnum[Offset]
    with LoggableEnum[Offset] {
  val values = findValues

  case object Earliest extends Offset

  case object Latest extends Offset

  import io.conduktor.graphql.implicits.generic._

  implicit val offsetSchema: GraphQLSchema[Offset]  = gen
  implicit val offsetArgBuilder: ArgBuilder[Offset] = ArgBuilder.gen

}
