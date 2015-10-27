package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class ScalaSdk(name: String,
                    languageLevel: String,
                    classes: Seq[Path] = Seq.empty,
                    sources: Seq[Path] = Seq.empty,
                    docs: Seq[Path] = Seq.empty,
                    compilerClasspath: Seq[Path] = Seq.empty)