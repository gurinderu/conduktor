package io.conduktor.util.implicits

import cats.syntax.monoid._
import doobie.util.log.{ExecFailure, LogEvent, ProcessingFailure, Success}
import org.apache.commons.codec.binary.Base64
import tofu.logging.{DictLoggable, HideLoggable, LogRenderer, Loggable}
import tofu.syntax.logRenderer._

object doobieLogEvent {
  implicit val logEventLoggable: Loggable[LogEvent] = new DictLoggable[LogEvent] with HideLoggable[LogEvent] {
    private def fmt(s: String): String = s.linesIterator.dropWhile(_.trim.isEmpty).mkString(" ")
    private def asStrings(a: List[Any]): List[String] =
      a.map {
        case x: Array[Byte] => Base64.encodeBase64String(x)
        case x              => x.toString
      }

    def fields[I, V, R, S](ev: LogEvent, i: I)(implicit r: LogRenderer[I, V, R, S]): R =
      ev match {
        case Success(s, a, e1, e2) =>
          i.field("sql-event-type", "Success") |+|
            i.field("sql-statement", fmt(s)) |+|
            i.field("sql-args", asStrings(a)) |+|
            i.field("sql-exec-ms", e1.toMillis) |+|
            i.field("sql-processing-ms", e2.toMillis) |+|
            i.field("sql-total-ms", (e1 + e2).toMillis)
        case ProcessingFailure(s, a, e1, e2, _) =>
          i.field("sql-event-type", "ProcessingFailure") |+|
            i.field("sql-statement", s) |+|
            i.field("sql-args", asStrings(a)) |+|
            i.field("sql-exec-ms", e1.toMillis) |+|
            i.field("sql-processing-ms", e2.toMillis) |+|
            i.field("sql-total-ms", (e1 + e2).toMillis)
        case ExecFailure(s, a, e1, _) =>
          i.field("sql-event-type", "ExecFailure") |+|
            i.field("sql-statement", s) |+|
            i.field("sql-args", asStrings(a)) |+|
            i.field("sql-exec-ms", e1.toMillis)
      }
  }
}
