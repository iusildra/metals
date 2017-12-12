package scala.meta.languageserver.compiler

import scala.reflect.internal.util.Position
import scala.tools.nsc.interactive.Global
import com.typesafe.scalalogging.LazyLogging

object CompilerUtils extends LazyLogging {
  def safeCompletionsAt[G <: Global](
      global: Global,
      position: Position
  ): List[global.CompletionResult#M] = {
    try {
      global.completionsAt(position).matchingResults().distinct
    } catch {
      case e: NullPointerException =>
        // do nothing, seems to happen regularly
        // java.lang.NullPointerException: null
        //   at scala.tools.nsc.Global$GlobalPhase.cancelled(Global.scala:408)
        //   at scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:418)
        //   at scala.tools.nsc.Global$Run.$anonfun$compileLate$3(Global.scala:1572)
        Nil
      case e: StringIndexOutOfBoundsException =>
        // NOTE(olafur) Let's log this for now while we are still learning more
        // about the PC. However, I haven't been able to avoid this exception
        // in some cases so I suspect it's here to stay until we fix it upstream.
        val stackTrace = e.getStackTrace.lift(4).map(_.toString).getOrElse("")
        logger.warn("Expected StringIndexOutOfBounds at " + stackTrace)
        Nil
    }
  }

}
