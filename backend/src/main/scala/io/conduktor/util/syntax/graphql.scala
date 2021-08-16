package io.conduktor.util.syntax

import caliban.{GraphQLInterpreter, Http4sAdapter}
import cats.effect.Blocker
import cats.syntax.semigroupk._
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.HttpRoutes
import zio.{Has, RIO}
import zio.interop.catz._
import zio.random.Random

import java.nio.file.Paths

object graphql {
  implicit class GraphQLIntrpOps[R <: Has[_] with Random, E](private val interpreter: GraphQLInterpreter[R, E])
      extends AnyVal {
    def asHttpService(blocker: Blocker, rootUploadPath: NonEmptyString): HttpRoutes[RIO[R, *]] =
      Http4sAdapter.makeHttpUploadService(interpreter, Paths.get(rootUploadPath.value), blocker) <+>
        Http4sAdapter.makeHttpService(interpreter)

    def asWsService: HttpRoutes[RIO[R, *]] = Http4sAdapter.makeWebSocketService(interpreter, skipValidation = false)
  }
}
