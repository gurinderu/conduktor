package io.conduktor.util.doobie

import io.conduktor.util.implicits.doobieLogEvent._
import cats.Functor
import doobie.util.log.{ExecFailure, LogEvent, ProcessingFailure, Success}
import tofu.doobie.log.{EmbeddableLogHandler, LogHandlerF}
import tofu.lift.UnliftIO
import tofu.logging.LoggingBase
import tofu.syntax.logging._

object logHandler {
  def makeEmbeddable[DB[_]: LoggingBase: Functor: UnliftIO]: EmbeddableLogHandler[DB] =
    EmbeddableLogHandler.async(handleEventWithLogging)

  private def handleEventWithLogging[F[_]: LoggingBase]: LogHandlerF[F] = {
    case ev: Success                           => debug"Successful statement execution ${ev: LogEvent}"
    case ev @ ProcessingFailure(_, _, _, _, t) => errorCause"Failed ResultSet processing ${ev: LogEvent}" (t)
    case ev @ ExecFailure(_, _, _, t)          => errorCause"Failed statement execution ${ev: LogEvent}" (t)
  }
}
