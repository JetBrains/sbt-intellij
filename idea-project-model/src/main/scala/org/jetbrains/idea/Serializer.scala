package org.jetbrains.idea

import org.jetbrains.idea.model._

import scala.xml.{NodeSeq, Node, Elem, Text}

/**
  * @author Pavel Fatin
  */
object Serializer {
  def toXml(project: Project): Elem =
    <project name={project.name} base={project.base}>
      {project.modules.map(toXml)}
      {project.libraries.map(toXml)}
      {project.scalaSdks.map(toXml)}
    </project>

  def fromXml(node: Node): Project =
    Project(
      name = (node \ "@name").text,
      base =(node \ "@base").text,
      modules = (node \ "module").map(moduleFrom),
      libraries = (node \ "library").map(libraryFrom),
      scalaSdks =  (node \ "scalaSdk").map(scalaSdkFrom))

  private def toXml(module: Module): Elem =
    <module name={module.name} kind={optional(module.kind, ModuleKind.Java)(format)}>
      {module.outputPaths.fold(NodeSeq.Empty)(toXml)}
      {module.contentRoots.map(toXml)}
      {module.moduleDependencies.map(toXml)}
      {module.libraryDependencies.map(toXml)}
      {module.libraries.map(toXml)}
      {module.sbtData.fold(NodeSeq.Empty)(toXml)}
    </module>

  private def moduleFrom(node: Node): Module =
    Module(
      name = (node \ "@name").text,
      kind = default((node \ "@kind").text, parseModuleKind, ModuleKind.Java),
      outputPaths = (node \ "paths").map(pathsFrom).headOption,
      contentRoots = (node \ "content").map(contentRootFrom),
      libraries = (node \ "library").map(moduleLevelLibraryFrom),
      moduleDependencies = (node \ "moduleDependency").map(moduleDependencyFrom),
      libraryDependencies = (node \ "libraryDependency").map(libraryDependencyFrom),
      sbtData = (node \ "sbt").map(sbtDataFrom).headOption)

  private def toXml(paths: OutputPaths): Elem =
    <paths exclude={optional(paths.exclude, true)(format)}>
      <production>{paths.production}</production>
      <test>{paths.test}</test>
    </paths>

  private def pathsFrom(node: Node): OutputPaths =
    OutputPaths(
      production = (node \ "production").text,
      test = (node \ "test").text,
      exclude = default((node \ "@exclude").text, parseBoolean, true))

  private def toXml(root: ContentRoot): Elem =
    <content root={root.base}>
      {root.sources.map(it => <sources>{it}</sources>)}
      {root.resources.map(it => <resources>{it}</resources>)}
      {root.testSources.map(it => <testSources>{it}</testSources>)}
      {root.testResources.map(it => <testResources>{it}</testResources>)}
      {root.excluded.map(it => <excluded>{it}</excluded>)}
    </content>

  private def contentRootFrom(node: Node): ContentRoot =
    ContentRoot(
      base = (node \ "@root").text,
      sources = (node \ "sources").map(_.text),
      resources = (node \ "resources").map(_.text),
      testSources = (node \ "testSources").map(_.text),
      testResources = (node \ "testResources").map(_.text),
      excluded = (node \ "excluded").map(_.text)
    )

  private def toXml(library: Library): Elem =
    <library name={library.name} resolved={optional(library.resolved, true)(format)}>
      {library.classes.map(it => <classes>{it}</classes>)}
      {library.sources.map(it => <sources>{it}</sources>)}
      {library.docs.map(it => <docs>{it}</docs>)}
    </library>

  private def libraryFrom(node: Node): Library =
    Library(
      name = (node \ "@name").text,
      resolved = default((node \ "@resolved").text, parseBoolean, true),
      classes = (node \ "classes").map(_.text),
      sources = (node \ "sources").map(_.text),
      docs = (node \ "docs").map(_.text)
    )

  private def toXml(dependency: ModuleDependency): Elem =
      <moduleDependency name={dependency.name} scope={optional(dependency.scope, Scope.Compile)(format)} exported={optional(dependency.exported, true)(format)} />

  private def moduleDependencyFrom(node: Node): ModuleDependency =
    ModuleDependency(
      name = (node \ "@name").text,
      scope = default((node \ "@scope").text, parseScope, Scope.Compile),
      exported = default((node \ "@exported").text, parseBoolean, true)
    )

  private def toXml(dependency: LibraryDependency): Elem =
    <libraryDependency name={dependency.name} level={optional(dependency.level, LibraryLevel.Project)(format)} scope={optional(dependency.scope, Scope.Compile)(format)} />

  private def libraryDependencyFrom(node: Node): LibraryDependency =
    LibraryDependency(
      name = (node \ "@name").text,
      level = default((node \ "@level").text, parseLibraryLevel, LibraryLevel.Project),
      scope = default((node \ "@scope").text, parseScope, Scope.Compile)
    )

  private def toXml(library: ModuleLevelLibrary): Elem =
    <library name={library.name} scope={optional(library.scope, Scope.Compile)(format)}>
      {library.classes.map(it => <classes>{it}</classes>)}
      {library.sources.map(it => <sources>{it}</sources>)}
      {library.docs.map(it => <docs>{it}</docs>)}
    </library>

  private def moduleLevelLibraryFrom(node: Node): ModuleLevelLibrary =
    ModuleLevelLibrary(
      name = (node \ "@name").text,
      scope = default((node \ "@scope").text, parseScope, Scope.Compile),
      classes = (node \ "classes").map(_.text),
      sources = (node \ "sources").map(_.text),
      docs = (node \ "docs").map(_.text)
    )

  private def toXml(sdk: ScalaSdk): Elem =
    <scalaSdk name={sdk.name} level={sdk.languageLevel}>
      {sdk.classes.map(it => <classes>{it}</classes>)}
      {sdk.sources.map(it => <sources>{it}</sources>)}
      {sdk.docs.map(it => <docs>{it}</docs>)}
      {sdk.compilerClasspath.map(it => <classpath>{it}</classpath>)}
    </scalaSdk>

  private def scalaSdkFrom(node: Node): ScalaSdk =
    ScalaSdk(
      name = (node \ "@name").text,
      languageLevel = (node \ "@level").text,
      classes = (node \ "classes").map(_.text),
      sources = (node \ "sources").map(_.text),
      docs = (node \ "docs").map(_.text),
      compilerClasspath = (node \ "classpath").map(_.text)
    )

  private def toXml(data: SbtData): Elem =
    <sbt>
      {data.imports.map(it => <import>{it}</import>)}
      {data.resolvers.map(toXml)}
    </sbt>

  private def sbtDataFrom(node: Node): SbtData =
    SbtData(
      imports = (node \ "import").map(_.text),
      resolvers = (node \ "resolver").map(resolverFrom)
    )

  private def toXml(resolver: Resolver): Elem =
    <resolver name={resolver.name} root={resolver.root}/>

  private def resolverFrom(node: Node): Resolver =
    Resolver(
      name = (node \ "@name").text,
      root = (node \ "@root").text)

  private def format(b: Boolean): String = b.toString

  private def parseBoolean(s: String): Boolean = s.toBoolean

  private def format(scope: Scope): String = scope match {
    case Scope.Compile => "compile"
    case Scope.Runtime => "runtime"
    case Scope.Test => "test"
    case Scope.Provided => "provided"
  }

  private def parseScope(s: String): Scope = s  match {
    case "compile" => Scope.Compile
    case "runtime" => Scope.Runtime
    case "test" => Scope.Test
    case "provided" => Scope.Provided
  }

  private def format(level: LibraryLevel): String = level match {
    case LibraryLevel.Application => "application"
    case LibraryLevel.Project => "project"
    case LibraryLevel.Module => "module"
  }

  private def parseLibraryLevel(s: String): LibraryLevel = s match {
    case "application" => LibraryLevel.Application
    case "project" => LibraryLevel.Project
    case "module" => LibraryLevel.Module
  }

  private def format(kind: ModuleKind): String = kind match {
    case ModuleKind.Java => "java"
    case ModuleKind.Sbt => "sbt"
    case ModuleKind.Source => "source"
  }

  private def parseModuleKind(s: String): ModuleKind = s match {
    case "java" => ModuleKind.Java
    case "sbt" => ModuleKind.Sbt
    case "source" => ModuleKind.Source
  }

  private def optional[T](v: T, z: T)(f: T => String): Option[Text] =
    Option(v).filterNot(_ == z).map(it => Text(f(it)))

  private def default[T](s: String, f: String => T, z: T): T =
    if (s.isEmpty) z else f(s)
}
