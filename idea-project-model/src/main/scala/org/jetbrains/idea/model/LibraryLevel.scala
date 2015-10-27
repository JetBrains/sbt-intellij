package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
sealed trait LibraryLevel

object LibraryLevel {
  case object Application extends LibraryLevel
  case object Project extends LibraryLevel
  case object Module extends LibraryLevel
}
