package org.jetbrains.idea.model

/**
  * @author Pavel Fatin
  */
case class ModuleLevelLibrary(name: String,
                              scope: Scope = Scope.Compile,
                              classes: Seq[Path] = Seq.empty,
                              sources: Seq[Path] = Seq.empty,
                              docs: Seq[Path] = Seq.empty)