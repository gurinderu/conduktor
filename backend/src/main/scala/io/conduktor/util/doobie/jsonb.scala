package io.conduktor.util.doobie

import cats.syntax.either._
import doobie.postgres.circe.jsonb.implicits._
import doobie.util.meta.Meta
import doobie.util.{Get, Put}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

object jsonb {
  def deriveJsonMeta[T: Encoder: Decoder]: Meta[T] = {
    val get = Get[Json].temap[T](_.as[T].leftMap(_.message))
    val put = Put[Json].tcontramap[T](_.asJson)
    new Meta[T](get, put)
  }
}
