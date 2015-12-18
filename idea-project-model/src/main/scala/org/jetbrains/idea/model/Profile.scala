package org.jetbrains.idea.model

/**
  * @author Pavel Fatin
  */
case class Profile(name: String, settings: ScalaCompilerSettings, modules: Seq[String])