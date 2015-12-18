package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
sealed trait DebuggingInfoLevel

object DebuggingInfoLevel {
  case object None extends DebuggingInfoLevel
  case object Source extends DebuggingInfoLevel
  case object Line extends DebuggingInfoLevel
  case object Vars extends DebuggingInfoLevel
  case object Notailcalls extends DebuggingInfoLevel
}
