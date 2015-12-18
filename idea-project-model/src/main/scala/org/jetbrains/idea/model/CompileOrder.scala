package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
sealed trait CompileOrder

object CompileOrder {
  case object Mixed extends CompileOrder
  case object JavaThenScala extends CompileOrder
  case object ScalaThenJava extends CompileOrder
}
