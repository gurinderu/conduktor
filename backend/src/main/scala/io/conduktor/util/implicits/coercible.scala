package io.conduktor.util.implicits

import cats.Show
import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import io.estatico.newtype.Coercible
import pureconfig.ConfigReader
import tofu.logging.Loggable

import scala.reflect.runtime.universe.TypeTag

object coercible {
  implicit def coercibleMeta[R, N](implicit ev: Coercible[Meta[R], Meta[N]], R: Meta[R]): Meta[N] = ev(R)

  implicit def coercibleTypeTag[R, N](implicit ev: Coercible[TypeTag[R], TypeTag[N]], R: TypeTag[R]): TypeTag[N] =
    ev(R)

  implicit def coercibleShow[R, N](implicit ev: Coercible[Show[R], Show[N]], R: Show[R]): Show[N] = ev(R)

  implicit def coercibleLoggable[R, N](implicit ev: Coercible[Loggable[R], Loggable[N]], R: Loggable[R]): Loggable[N] =
    ev(R)

  implicit def coercibleDecoder[R, N](implicit ev: Coercible[Decoder[R], Decoder[N]], R: Decoder[R]): Decoder[N] = ev(R)

  implicit def coercibleEncoder[R, N](implicit ev: Coercible[Encoder[R], Encoder[N]], R: Encoder[R]): Encoder[N] = ev(R)

  implicit def coercibleKeyDecoder[R, N](implicit
    ev: Coercible[KeyDecoder[R], KeyDecoder[N]],
    R: KeyDecoder[R]
  ): KeyDecoder[N] = ev(R)

  implicit def coercibleKeyEncoder[R, N](implicit
    ev: Coercible[KeyEncoder[R], KeyEncoder[N]],
    R: KeyEncoder[R]
  ): KeyEncoder[N] = ev(R)

  implicit def coercibleConfigReader[R, N](implicit
    ev: Coercible[ConfigReader[R], ConfigReader[N]],
    R: ConfigReader[R]
  ): ConfigReader[N] = ev(R)
}
