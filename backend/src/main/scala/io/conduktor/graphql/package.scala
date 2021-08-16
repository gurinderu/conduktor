package io.conduktor

import caliban.CalibanError.ExecutionError
import caliban.GraphQLInterpreter
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import caliban.uploads.Uploads
import cats.data.NonEmptyList
import io.conduktor.context.ServiceCtx
import io.conduktor.util.implicits.graphql.{CoercibleSchema, RefinedSchema}
import zio.clock.Clock
import zio.random.Random
import zio.stream.ZStream
import zio.{Has, RIO, Task}

package object graphql {
  type GraphQLCtx            = Has[ServiceCtx[Task]] with Clock with Random with Uploads
  type GraphQLEffect[x]      = RIO[GraphQLCtx, x]
  type GraphQLSchema[T]      = Schema[GraphQLCtx, T]
  type GraphQLStream[T]      = ZStream[GraphQLCtx, Throwable, T]
  type GraphQLCtxInterpreter = GraphQLInterpreter[GraphQLCtx, Throwable]

  object implicits {
    object generic extends GenericSchema[GraphQLCtx] {
      implicit def nelSchema[R, T](implicit S: Schema[R, T]): Schema[R, NonEmptyList[T]] =
        listSchema[R, T].contramap(_.toList)

      implicit def nelArgBuilder[T](implicit A: ArgBuilder[T]): ArgBuilder[NonEmptyList[T]] =
        ArgBuilder.list[T].flatMap(l => NonEmptyList.fromList(l).toRight(ExecutionError("List should not be empty")))
    }

    object coercible extends CoercibleSchema[GraphQLCtx]

    object refined extends RefinedSchema[GraphQLCtx]
  }

}
