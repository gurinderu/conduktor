package io.conduktor.util.implicits

import eu.timepit.refined.api.Refined
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

object chimney {
  implicit def refinedTransformer[TIn, TOut, P](implicit
    T: Transformer[TIn, TOut]
  ): Transformer[Refined[TIn, P], Refined[TOut, P]] =
    (src: Refined[TIn, P]) => Refined.unsafeApply[TOut, P](src.value.transformInto)

}
