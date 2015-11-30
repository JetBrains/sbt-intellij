package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class Project(name: String,
                   base: Path,
                   modules: Seq[Module] = Seq.empty,
                   libraries: Seq[Library] = Seq.empty)