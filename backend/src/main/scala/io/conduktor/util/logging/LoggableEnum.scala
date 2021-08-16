package io.conduktor.util.logging

import enumeratum.{Enum, EnumEntry}
import tofu.logging.Loggable

trait LoggableEnum[A <: EnumEntry] { this: Enum[A] =>
  implicit val enumLoggable: Loggable[A] = Loggable.stringValue.contramap(_.entryName)
}
