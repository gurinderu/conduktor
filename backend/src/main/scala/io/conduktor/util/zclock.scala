package io.conduktor.util

import java.time.{DateTimeException, OffsetDateTime}
import java.util.concurrent.TimeUnit

import cats.Apply
import cats.effect.{Clock, Timer}
import tofu.common.TimeZone
import tofu.lift.Lift
import tofu.syntax.lift._
import tofu.syntax.time.now
import zio.{IO, Task, UIO}
import zio.clock.{Clock => ZClock}
import zio.duration.{Duration => ZDuration}

import scala.concurrent.duration.{Duration, NANOSECONDS}

object zclock {
  def make[F[_]: Apply: Timer: TimeZone: Lift[*[_], Task]]: ZClock.Service =
    new ZClock.Service {
      def currentTime(unit: TimeUnit): UIO[Long] = Clock[F].realTime(unit).lift[Task].orDie
      def currentDateTime: IO[DateTimeException, OffsetDateTime] =
        now[F, OffsetDateTime].lift[Task].refineOrDie { case t: DateTimeException =>
          t
        }
      def nanoTime: UIO[Long] = Clock[F].monotonic(NANOSECONDS).lift[Task].orDie
      def sleep(duration: ZDuration): UIO[Unit] =
        duration match {
          case ZDuration.Finite(nanos) => Timer[F].sleep(Duration.fromNanos(nanos)).lift[Task].orDie
          case _                       => UIO.never
        }
    }
}
