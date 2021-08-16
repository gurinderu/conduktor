package io.conduktor

import cats.effect.{ConcurrentEffect, Timer}
import io.conduktor.context.ServiceCtx
import org.log4s.{getLogger, Logger}
import tofu.lift.{IsoK, Lift}
import zio.{ExitCode, URIO}
import tofu.zioInstances.implicits._
import zio._
import zio.internal.Platform
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends zio.App with AppF {
  lazy val logger: Logger = getLogger

  override val platform: Platform = Platform.default
    .withReportFatal { t =>
      logger.error(t)("FATALITY!")
      throw t
    }
    .withReportFailure { cause =>
      cause.dieOption.fold(logger.error(cause.prettyPrint))(
        logger.error(_)(cause.prettyPrint)
      )
    }

  def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    type I[x] = Task[x]
    type F[x] = RIO[ServiceCtx[I], x]

    for {
      implicit0(ce: ConcurrentEffect[I]) <- Task.concurrentEffect
      implicit0(timerF: Timer[F])    = Timer[I].mapK(Lift.trans[I, F])
      implicit0(isoTask: IsoTask[I]) = IsoK.id[I]
      _ <- runF[I, F].orDie
    } yield ExitCode.success
  }
}
