package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class Project(name: String,
                   base: Path,
                   jdk: Option[String] = None,
                   languageLevel: Option[JavaLanguageLevel] = None,
                   modules: Seq[Module] = Seq.empty,
                   libraries: Seq[Library] = Seq.empty,
                   profiles: Seq[Profile] = Seq.empty)