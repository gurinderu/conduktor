package io.conduktor.util.syntax

import tofu.higherKind.{Function2K, Mid, MonoidalK}
import tofu.syntax.monoidalK._

object mid {
  implicit class MidOps[U[_[_]], F[_]](private val x: U[Mid[F, *]]) extends AnyVal {
    def >>>(y: U[Mid[F, *]])(implicit U: MonoidalK[U]): U[Mid[F, *]] = (x zipWithKTo y)(Function2K(_ andThen _))
  }
}
