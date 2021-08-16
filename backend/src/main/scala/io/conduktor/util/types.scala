package io.conduktor.util

import _root_.cats.Show
import _root_.cats.syntax.either._
import _root_.pureconfig.ConfigReader
import _root_.pureconfig.error.FailureReason
import io.circe.Decoder
import io.estatico.newtype.macros.newtype
import tofu.logging.Loggable

object types {
  @newtype final case class Secret(value: Array[Byte])

  object Secret {
    implicit val show: Show[Secret]         = Show.show(_ => "secret(**********)")
    implicit val loggable: Loggable[Secret] = Loggable.show
    implicit val reader: ConfigReader[Secret] = ConfigReader.fromString(Secret(_).leftMap { ex =>
      new FailureReason {
        def description: String = ex
      }
    })
    implicit val decoder: Decoder[Secret] = Decoder.decodeString.emap(Secret(_))

    def unsafeFrom(raw: String): Secret = apply(raw).valueOr(msg => throw new IllegalArgumentException(msg))

    def apply(raw: String): Either[String, Secret] =
      if (raw.nonEmpty)
        Secret(raw.getBytes()).asRight[String]
      else
        s"Secret could not be empty".asLeft

  }
}
