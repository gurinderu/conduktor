package io.conduktor

import cats.Id
import doobie.util.log.LogHandler
import tofu.WithProvide

package object repo {
  type ProvideLogHandler[F[_]] = WithProvide[F, Id, LogHandler]
}
