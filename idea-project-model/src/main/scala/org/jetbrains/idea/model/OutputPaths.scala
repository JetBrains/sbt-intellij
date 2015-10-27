package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class OutputPaths(production: Path,
                       test: Path,
                       exclude: Boolean = true)
