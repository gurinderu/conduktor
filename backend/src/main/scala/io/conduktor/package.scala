package io

import io.conduktor.context.ServiceCtx
import tofu.WithRun
import tofu.lift.IsoK
import tofu.logging.Logs
import zio.Task

package object conduktor {
  type RunServiceCtx[F[_], I[_]] = WithRun[F, I, ServiceCtx[I]]
  type IsoTask[I[_]]             = IsoK[I, Task]
  type LogsSame[F[_]]            = Logs[F, F]

}
