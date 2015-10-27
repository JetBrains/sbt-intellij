package org.jetbrains.idea

import org.jetbrains.idea.model._

import scala.xml.{Elem, Text}

/**
  * @author Pavel Fatin
  */
object Serializer {
  def toXml(project: Project): Elem =
    <project name={project.name} base={project.base}>
      {project.modules.map(toXml)}
      {project.libraries.map(toXml)}
    </project>

  private def toXml(module: Module): Elem =
    <module name={module.name} kind={optional(module.kind, ModuleKind.Java)(format)}>
      {module.outputPaths.toSeq.map(toXml)}
      {module.contentRoots.map(toXml)}
      {module.libraryDependencies.map(toXml)}
      {module.moduleDependencies.map(toXml)}
      {module.libraries.map(toXml)}
    </module>

  private def toXml(paths: OutputPaths): Elem =
      <paths exclude={optional(paths.exclude, true)(format)}>
        <production>{paths.production}</production>
        <test>{paths.test}</test>
      </paths>

  private def toXml(root: ContentRoot): Elem =
      <content root={root.base}>
        {root.sources.map(it => <sources>{it}</sources>)}
        {root.resources.map(it => <resources>{it}</resources>)}
        {root.testSources.map(it => <testSources>{it}</testSources>)}
        {root.testResources.map(it => <testResources>{it}</testResources>)}
        {root.excluded.map(it => <excluded>{it}</excluded>)}
      </content>

  private def toXml(dependency: LibraryDependency): Elem =
      <libraryDependency name={dependency.name} level={optional(dependency.level, LibraryLevel.Project)(format)} scope={optional(dependency.scope, Scope.Compile)(format)} />

  private def toXml(library: Library): Elem =
    <library name={library.name} resolved={optional(library.resolved, true)(format)}>
      {library.classes.map(it => <classes>{it}</classes>)}
      {library.sources.map(it => <sources>{it}</sources>)}
      {library.docs.map(it => <docs>{it}</docs>)}
    </library>

  private def toXml(library: ModuleLevelLibrary): Elem =
    <library name={library.name} scope={optional(library.scope, Scope.Compile)(format)}>
      {library.classes.map(it => <classes>{it}</classes>)}
      {library.sources.map(it => <sources>{it}</sources>)}
      {library.docs.map(it => <docs>{it}</docs>)}
    </library>

  private def toXml(dependency: ModuleDependency): Elem =
      <moduleDependency name={dependency.name} scope={optional(dependency.scope, Scope.Compile)(format)} exported={optional(dependency.exported, true)(format)} />

  private def format(b: Boolean): String = b.toString

  private def format(scope: Scope): String = scope match {
    case Scope.Compile => "compile"
    case Scope.Runtime => "runtime"
    case Scope.Test => "test"
    case Scope.Provided => "provided"
  }

  private def format(level: LibraryLevel): String = level match {
    case LibraryLevel.Application => "application"
    case LibraryLevel.Project => "project"
    case LibraryLevel.Module => "module"
  }

  private def format(kind: ModuleKind): String = kind match {
    case ModuleKind.Java => "java"
    case ModuleKind.Sbt => "sbt"
    case ModuleKind.Source => "source"
  }

  private def optional[T](v: T, z: T)(f: T => String): Option[Text] =
    Option(v).filterNot(_ == z).map(it => Text(f(it)))
}
