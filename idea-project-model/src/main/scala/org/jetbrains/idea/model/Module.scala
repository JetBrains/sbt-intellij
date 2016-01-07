package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class Module(name: String,
                  kind: ModuleKind = ModuleKind.Java,
                  jdk: Option[String] = None,
                  languageLevel: Option[String] = None,
                  outputPaths: Option[OutputPaths] = None,
                  contentRoots: Seq[ContentRoot] = Seq.empty,
                  libraries: Seq[ModuleLevelLibrary] = Seq.empty,
                  moduleDependencies: Seq[ModuleDependency] = Seq.empty,
                  libraryDependencies: Seq[LibraryDependency] = Seq.empty,
                  sbtData: Option[SbtData] = None)
