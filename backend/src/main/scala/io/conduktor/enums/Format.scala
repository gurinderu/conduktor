package io.conduktor.enums

import caliban.schema.ArgBuilder
import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{CatsEnum, CirceEnum, DoobieEnum, Enum, EnumEntry}
import io.conduktor.graphql.GraphQLSchema
import io.conduktor.util.logging.LoggableEnum

sealed trait Format extends EnumEntry with UpperSnakecase

object Format
    extends Enum[Format]
    with CatsEnum[Format]
    with CirceEnum[Format]
    with DoobieEnum[Format]
    with LoggableEnum[Format] {

  import io.conduktor.graphql.implicits.generic._

  val values = findValues

  case object `String` extends Format

  case object `JSON` extends Format

  implicit val formatSchema: GraphQLSchema[Format]  = gen
  implicit val formatArgBuilder: ArgBuilder[Format] = ArgBuilder.gen

}
