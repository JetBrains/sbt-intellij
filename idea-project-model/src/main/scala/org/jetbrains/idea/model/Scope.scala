package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
sealed trait Scope

object Scope {
  case object Compile extends Scope
  case object Runtime extends Scope
  case object Test extends Scope
  case object Provided extends Scope
}
