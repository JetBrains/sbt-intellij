package org.jetbrains.idea

import org.jetbrains.idea.model._

import scala.xml.{NodeSeq, Node, Elem, Text}

/**
  * @author Pavel Fatin
  */
object Serializer {
  def toXml(project: Project): Elem =
    <project name={project.name} base={project.base} jdk={text(project.jdk)} languageLevel={text(project.languageLevel)}>
      {project.modules.map(toXml)}
      {project.libraries.map(toXml)}
      {project.profiles.map(toXml)}
    </project>

  def fromXml(node: Node): Project =
    Project(
      name = (node \ "@name").text,
      base =(node \ "@base").text,
      jdk = (node \ "@jdk").headOption.map(_.text),
      languageLevel = (node \ "@languageLevel").headOption.map(_.text),
      modules = (node \ "module").map(moduleFrom),
      libraries = (node \ "library").map(libraryFrom),
      profiles = (node \ "profile").map(profileFrom))

  private def toXml(module: Module): Elem =
    <module name={module.name} kind={optional(module.kind, ModuleKind.Java)(format)} jdk={text(module.jdk)} languageLevel={text(module.languageLevel)}>
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
      jdk = (node \ "@jdk").headOption.map(_.text),
      languageLevel = (node \ "@languageLevel").headOption.map(_.text),
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
      {library.scalaCompiler.fold(NodeSeq.Empty)(toXml)}
    </library>

  private def toXml(compiler: ScalaCompiler): Elem =
    <scalaCompiler level={compiler.level}>
      {compiler.classpath.map(it => <classpath>{it}</classpath>)}
    </scalaCompiler>

  private def libraryFrom(node: Node): Library =
    Library(
      name = (node \ "@name").text,
      resolved = default((node \ "@resolved").text, parseBoolean, true),
      classes = (node \ "classes").map(_.text),
      sources = (node \ "sources").map(_.text),
      docs = (node \ "docs").map(_.text),
      scalaCompiler = (node \ "scalaCompiler").map(scalaCompilerFrom).headOption
    )

  private def scalaCompilerFrom(node: Node): ScalaCompiler =
    ScalaCompiler(
      level = (node \ "@level").text,
      classpath = (node \ "classpath").map(_.text)
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
    <resolver name={resolver.name} kind={resolver.kind} root={resolver.root}/>

  private def resolverFrom(node: Node): Resolver =
    Resolver(
      name = (node \ "@name").text,
      kind = (node \ "@kind").text,
      root = (node \ "@root").text)

  private def toXml(profile: Profile): Elem =
    <profile name={profile.name} modules={profile.modules.mkString(",")}>
      {toXml(profile.settings).child}
    </profile>

  private def profileFrom(node: Node): Profile =
    Profile(
      name = (node \ "@name").text,
      settings = scalaCompilerSettingsFrom(node),
      modules = (node \ "@modules").text.split(",").toSeq
    )

  private def toXml(settings: ScalaCompilerSettings): Node =
    <profile>
      {if (settings.compileOrder != CompileOrder.Mixed)
        <compileOrder>{format(settings.compileOrder)}</compileOrder>
      }
      {if (settings.dynamics) <dynamics>true</dynamics>}
      {if (settings.postfixOps) <postfixOps>true</postfixOps>}
      {if (settings.reflectiveCalls) <reflectiveCalls>true</reflectiveCalls>}
      {if (settings.implicitConversions) <implicitConversions>true</implicitConversions>}
      {if (settings.higherKinds) <higherKinds>true</higherKinds>}
      {if (settings.existentials) <existentials>true</existentials>}
      {if (settings.macros) <macros>true</macros>}
      {if (settings.experimental) <experimental>true</experimental>}
      {if (!settings.warnings) <warnings>false</warnings>}
      {if (settings.deprecationWarnings) <deprecationWarnings>true</deprecationWarnings>}
      {if (settings.uncheckedWarnings) <uncheckedWarnings>true</uncheckedWarnings>}
      {if (settings.featureWarnings) <featureWarnings>true</featureWarnings>}
      {if (settings.optimiseBytecode) <optimiseBytecode>true</optimiseBytecode>}
      {if (settings.explainTypeErrors) <explainTypeErrors>true</explainTypeErrors>}
      {if (!settings.specialization) <specialization>false</specialization>}
      {if (settings.continuations) <continuations>true</continuations>}
      {if (settings.debuggingInfoLevel != DebuggingInfoLevel.Vars)
        <debuggingInfoLevel>{format(settings.debuggingInfoLevel)}</debuggingInfoLevel>
      }
      {settings.additionalCompilerOptions.map(it => <option>{it}</option>)}
      {settings.plugins.map(it => <plugin>{it}</plugin>)}
    </profile>

  private def scalaCompilerSettingsFrom(node: Node): ScalaCompilerSettings =
    ScalaCompilerSettings(
      compileOrder = default((node \ "compileOrder").text, parseCompileOrder, CompileOrder.Mixed),
      dynamics = default((node \ "dynamics").text, parseBoolean, false),
      postfixOps = default((node \ "postfixOps").text, parseBoolean, false),
      reflectiveCalls = default((node \ "reflectiveCalls").text, parseBoolean, false),
      implicitConversions = default((node \ "implicitConversions").text, parseBoolean, false),
      higherKinds = default((node \ "higherKinds").text, parseBoolean, false),
      existentials = default((node \ "existentials").text, parseBoolean, false),
      macros = default((node \ "macros").text, parseBoolean, false),
      experimental = default((node \ "experimental").text, parseBoolean, false),
      warnings = default((node \ "warnings").text, parseBoolean, true),
      deprecationWarnings = default((node \ "deprecationWarnings").text, parseBoolean, false),
      uncheckedWarnings = default((node \ "uncheckedWarnings").text, parseBoolean, false),
      featureWarnings = default((node \ "featureWarnings").text, parseBoolean, false),
      optimiseBytecode = default((node \ "optimiseBytecode").text, parseBoolean, false),
      explainTypeErrors = default((node \ "explainTypeErrors").text, parseBoolean, false),
      specialization = default((node \ "specialization").text, parseBoolean, true),
      continuations = default((node \ "continuations").text, parseBoolean, false),
      debuggingInfoLevel = default((node \ "debuggingInfoLevel").text, parseDebuggingInfoLevel, DebuggingInfoLevel.Vars),
      additionalCompilerOptions = (node \ "option").map(_.text),
      plugins = (node \ "plugin").map(_.text)
    )

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

  private def format(kind: CompileOrder): String = kind match {
    case CompileOrder.Mixed => "mixed"
    case CompileOrder.JavaThenScala => "javaThenScala"
    case CompileOrder.ScalaThenJava => "scalaThenJava"
  }

  private def parseCompileOrder(s: String): CompileOrder = s match {
    case "mixed" => CompileOrder.Mixed
    case "javaThenScala" => CompileOrder.JavaThenScala
    case "scalaThenJava" => CompileOrder.ScalaThenJava
  }

  private def format(kind: DebuggingInfoLevel): String = kind match {
    case DebuggingInfoLevel.None => "none"
    case DebuggingInfoLevel.Source => "source"
    case DebuggingInfoLevel.Line => "line"
    case DebuggingInfoLevel.Vars => "vars"
    case DebuggingInfoLevel.Notailcalls => "notailcalls"
  }

  private def parseDebuggingInfoLevel(s: String): DebuggingInfoLevel = s match {
    case "none" => DebuggingInfoLevel.None
    case "source" => DebuggingInfoLevel.Source
    case "line" => DebuggingInfoLevel.Line
    case "vars" => DebuggingInfoLevel.Vars
    case "notailcalls" => DebuggingInfoLevel.Notailcalls
  }

  private def optional[T](v: T, z: T)(f: T => String): Option[Text] =
    Option(v).filterNot(_ == z).map(it => Text(f(it)))

  private def text(v: Option[Any]): Option[Text] =
    v.map(it => Text(it.toString))

  private def default[T](s: String, f: String => T, z: T): T =
    if (s.isEmpty) z else f(s)
}
