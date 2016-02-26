package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class ScalaCompiler(level: ScalaLanguageLevel,
                         classpath: Seq[Path] = Seq.empty)