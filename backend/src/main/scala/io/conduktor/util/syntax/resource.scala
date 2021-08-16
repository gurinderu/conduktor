package io.conduktor.util.syntax

import cats.Applicative
import cats.syntax.applicative._
import cats.effect.Resource
import tofu.BracketThrow

object resource {
  implicit final class ToResourceOps[F[_], A](private val fa: F[A]) extends AnyVal {
    def toResource(implicit F: Applicative[F]): Resource[F, A] = Resource.eval(fa)
  }

  implicit final class ResourceOps[F[_], A](private val fa: Resource[F, A]) extends AnyVal {
    def run(implicit F: BracketThrow[F]): F[A]                   = fa.use(_.pure[F])
    def runWith[B](f: A => B)(implicit F: BracketThrow[F]): F[B] = fa.use(f(_).pure[F])
  }
}
