package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class Module(name: String,
                  kind: ModuleKind = ModuleKind.Java,
                  outputPaths: Option[OutputPaths] = None,
                  contentRoots: Seq[ContentRoot] = Seq.empty,
                  libraries: Seq[ModuleLevelLibrary] = Seq.empty,
                  libraryDependencies: Seq[LibraryDependency] = Seq.empty,
                  moduleDependencies: Seq[ModuleDependency] = Seq.empty,
                  sbtData: Option[SbtData] = None)
