package scala.meta.internal.metals.debug

import org.eclipse.lsp4j.debug.LaunchRequestArguments

class ScalaDebugLaunchRequestArguments(val stepFilers: Array[String])
    extends LaunchRequestArguments
