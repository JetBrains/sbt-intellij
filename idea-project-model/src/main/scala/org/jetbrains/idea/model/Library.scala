package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class Library(name: String,
                   classes: Seq[Path] = Seq.empty,
                   sources: Seq[Path] = Seq.empty,
                   docs: Seq[Path] = Seq.empty,
                   resolved: Boolean = true)