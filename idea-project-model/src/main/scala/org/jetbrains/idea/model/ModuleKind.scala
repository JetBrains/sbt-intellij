package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
sealed trait ModuleKind

object ModuleKind {
  case object Java extends ModuleKind
  case object Sbt extends ModuleKind
  case object Source extends ModuleKind
}
