package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class LibraryDependency(name: String,
                             level: LibraryLevel = LibraryLevel.Project,
                             scope: Scope = Scope.Compile)