package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class ScalaCompiler(level: String,
                         classpath: Seq[Path] = Seq.empty)