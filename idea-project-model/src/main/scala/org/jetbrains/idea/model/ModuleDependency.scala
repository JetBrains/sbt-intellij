package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class ModuleDependency(name: String,
                            scope: Scope = Scope.Compile,
                            exported: Boolean = true)