package org.jetbrains.sbt.converter

import java.io.File

import org.jetbrains.idea.model._
import org.jetbrains.sbt.{structure => sbtStructure}

/**
 * @author Pavel Fatin
 */
object Converter {
  def convert(root: Path, data: sbtStructure.StructureData, jdk: Option[String]): Project = {
    val projects = data.projects

    val project = data.projects.find(p => FileUtil.filesEqual(p.base, new File(root)))
      .orElse(data.projects.headOption)
      .getOrElse(throw new RuntimeException("No root project found"))

//    val basePackages = projects.flatMap(_.basePackages).distinct
//    val javacOptions = project.java.map(_.options).getOrElse(Seq.empty)
//    val sbtVersion = data.sbtVersion
//    val projectJdk = project.android.map(android => Android(android.targetVersion))
//      .orElse(jdk.map(JdkByVersion))

//    projectNode.add(new SbtProjectNode(basePackages, projectJdk, javacOptions, sbtVersion, root))

//    project.play2 map {
//      case play2Data =>
//        import Play2Keys.AllKeys._
//        val oldPlay2Data = play2Data.keys.map { case sbtStructure.Play2Key(name, values) =>
//          val newVals = values.mapValues {
//            case sbtStructure.PlayString(str) => new StringParsedValue(str)
//            case sbtStructure.PlaySeqString(strs) => new SeqStringParsedValue(strs)
//          }
//          def avoidSL7005Bug[K, V](m: Map[K,V]): Map[K, V] = HashMap(m.toSeq:_*)
//          (name, avoidSL7005Bug(newVals))
//        }
//        projectNode.add(new Play2ProjectNode(oldPlay2Data.toMap))
//    }

    val libraries = createLibraries(data, projects)

    val modules = createModules(projects, libraries)

    val scalaVersionToClasspath = projects.flatMap(_.scala.map(scala =>
      (scala.version, scala.libraryJar +: scala.compilerJar +: scala.extraJars)))

    val librariesAndSdks = libraries.map { library =>
      scalaVersionToClasspath.find {
        case (version, _) => library.name.contains(version)
      }.map {
        case (version, classpath) => library.copy(scalaCompiler = Some(ScalaCompiler(version, classpath.map(_.getPath))))
      } getOrElse {
        library
      }
    }

//    val projectToModuleNode: Map[sbtStructure.ProjectData, ModuleNode] = projects.zip(moduleNodes).toMap
//    val sharedSourceModules = createSharedSourceModules(projectToModuleNode, libraryNodes, moduleFilesDirectory)

    val buildModules = projects.map(createBuildModule(_, data.localCachePath))

    Project(
      name = project.name,
      base = root,
      modules = (modules ++ buildModules).sortBy(_.name),
      libraries = librariesAndSdks.sortBy(_.name))
  }

  def createModules(projects: Seq[sbtStructure.ProjectData], libraries: Seq[Library]): Seq[Module] = {
    projects.map { project =>
      val productionOutputPath = project.configurations.find(_.id == "compile").map(_.classes.path)
      val testOutputPath = project.configurations.find(_.id == "test").map(_.classes.path)

      val outputPaths = (productionOutputPath, testOutputPath).zipped.map(OutputPaths(_, _)).headOption

//      moduleNode.add(createModuleExtData(project))
//      moduleNode.addAll(project.android.map(createFacet(project, _)).toSeq)

      val libraryDependencies = createLibraryDependencies(project.dependencies.modules)
//      :+ LibraryDependency(Sbt.UnmanagedSourcesAndDocsName) // TODO

      val moduleDependencies = project.dependencies.projects.map(dependencyId =>
        ModuleDependency(dependencyId.project, scopeFor(dependencyId.configuration)))

      Module(
        name = project.id,
        kind = ModuleKind.Java,
        outputPaths = outputPaths,
        contentRoots = Seq(createContentRoot(project)),
        libraryDependencies = libraryDependencies.sortBy(_.name),
        moduleDependencies = moduleDependencies.sortBy(_.name),
        libraries = createUnmanagedDependencies(project.dependencies.jars).sortBy(_.name)
      )
    }
  }

  def createLibraries(data: sbtStructure.StructureData, projects: Seq[sbtStructure.ProjectData]): Seq[Library] = {
    val repositoryModules = data.repository.map(_.modules).getOrElse(Seq.empty)
    val (modulesWithoutBinaries, modulesWithBinaries) = repositoryModules.partition(_.binaries.isEmpty)
    val otherModuleIds = projects.flatMap(_.dependencies.modules.map(_.id)).toSet --
      repositoryModules.map(_.id).toSet

    val libs = modulesWithBinaries.map(createResolvedLibrary) ++ otherModuleIds.map(createUnresolvedLibrary)

    val modulesWithDocumentation = modulesWithoutBinaries.filter(m => m.docs.nonEmpty || m.sources.nonEmpty)
    if (modulesWithDocumentation.isEmpty) return libs

    val unmanagedSourceLibrary = Library(
      name = Sbt.UnmanagedSourcesAndDocsName,
      sources = modulesWithDocumentation.flatMap(_.sources).map(_.path),
      docs = modulesWithDocumentation.flatMap(_.docs).map(_.path))

    libs :+ unmanagedSourceLibrary
  }

//  private def createModuleExtData(project: sbtStructure.ProjectData): ModuleExtNode = {
//    val scalaVersion = project.scala.map(s => Version(s.version))
//    val scalacClasspath = project.scala.fold(Seq.empty[File])(s => s.compilerJar +: s.libraryJar +: s.extraJars)
//    val scalacOptions = project.scala.fold(Seq.empty[String])(_.options)
//    val javacOptions = project.java.fold(Seq.empty[String])(_.options)
//    val jdk = project.android.map(android => Android(android.targetVersion))
//      .orElse(project.java.flatMap(java => java.home.map(JdkByHome)))
//    new ModuleExtNode(scalaVersion, scalacClasspath, scalacOptions, jdk, javacOptions)
//  }

//  private def createAndroidFacet(project: sbtStructure.ProjectData, android: sbtStructure.AndroidData): AndroidFacet = {
//    new AndroidFacet(
//      targetVersion = android.targetVersion,
//      manifest = android.manifestPath,
//      apk = android.apkPath,
//      resources = android.resPath,
//      assets = android.assetsPath,
//      generator = android.genPath,
//      libraries = android.libsPath,
//      isLibrary = android.isLibrary,
//      proguardConfig = android.proguardConfig)
//  }

  private def createUnresolvedLibrary(moduleId: sbtStructure.ModuleIdentifier): Library = {
    val module = sbtStructure.ModuleData(moduleId, Set.empty, Set.empty, Set.empty)
    createLibrary(module, resolved = false)
  }

  private def createResolvedLibrary(module: sbtStructure.ModuleData): Library = {
    createLibrary(module, resolved = true)
  }

  private def createLibrary(module: sbtStructure.ModuleData, resolved: Boolean): Library = {
    Library(
      name = nameFor(module.id),
      classes = module.binaries.map(_.path).toSeq.sorted,
      sources = module.sources.map(_.path).toSeq.sorted,
      docs = module.docs.map(_.path).toSeq.sorted,
      resolved = resolved)
  }

  private def nameFor(id: sbtStructure.ModuleIdentifier) = {
    val classifierOption = if (id.classifier.isEmpty) None else Some(id.classifier)
    s"${id.organization}:${id.name}:${id.revision}" + classifierOption.map(":"+_).getOrElse("") + s":${id.artifactType}"
  }

  private def createContentRoot(project: sbtStructure.ProjectData): ContentRoot = {
    val productionSources = validRootPathsIn(project, "compile")(_.sources)
    val productionResources = validRootPathsIn(project, "compile")(_.resources)
    val testSources = validRootPathsIn(project, "test")(_.sources) ++ validRootPathsIn(project, "it")(_.sources)
    val testResources = validRootPathsIn(project, "test")(_.resources) ++ validRootPathsIn(project, "it")(_.resources)

    val excludedDirectories = getExcludedTargetDirs(project).map(_.path)

    ContentRoot(
      base = project.base.path,
      sources = productionSources.sorted,
      resources = productionResources.sorted,
      testSources = testSources.sorted,
      testResources = testResources.sorted,
      excluded = excludedDirectories.sorted)
  }

  // We cannot always exclude the whole ./target/ directory because of
  // the generated sources, so we resort to an heuristics.
  private def getExcludedTargetDirs(project: sbtStructure.ProjectData): Seq[File] = {
    val extractedExcludes = project.configurations.flatMap(_.excludes)
    if (extractedExcludes.nonEmpty)
      return extractedExcludes.distinct

    val managedDirectories = project.configurations
      .flatMap(configuration => configuration.sources ++ configuration.resources)
      .filter(_.managed)
      .map(_.file)

    val defaultNames = Set("main", "test")

    val relevantDirectories = managedDirectories.filter(file => file.exists || !defaultNames.contains(file.getName))
    def isRelevant(f: File): Boolean = !relevantDirectories.forall(_.isOutsideOf(f))

    if (isRelevant(project.target)) {
      // If we can't exclude the target directory, go one level deeper (which may hit resolution-cache and streams)
      Option(project.target.listFiles()).toList.flatten.filter {
        child => child.isDirectory && !isRelevant(child)
      }
    } else List(project.target)
  }

  private def createBuildModule(project: sbtStructure.ProjectData, localCachePath: Option[String]): Module = {
    val id = project.id + Sbt.BuildModuleSuffix
    val name = project.name + Sbt.BuildModuleSuffix
    val buildRoot = project.base.path / Sbt.ProjectDirectory

    val outputPaths = OutputPaths(
      buildRoot / Sbt.TargetDirectory / "idea-classes",
      buildRoot / Sbt.TargetDirectory / "idea-test-classes")

    val root = createBuildContentRoot(buildRoot)

    val library = {
      val build = project.build
      val classes = build.classes.filter(_.exists).map(_.path)
      val docs = build.docs.filter(_.exists).map(_.path)
      val sources = build.sources.filter(_.exists).map(_.path)

      createModuleLevelLibrary(
        name = Sbt.BuildLibraryName,
        classes = classes.sorted,
        docs = docs.sorted,
        sources = sources.sorted,
        scope = Scope.Compile)
    }

    val sbtData = createSbtModuleData(project, localCachePath)

    new Module(name = id, kind = ModuleKind.Sbt, contentRoots = Seq(root), libraries = Seq(library), outputPaths = Some(outputPaths), sbtData = Some(sbtData))
  }

  private def createBuildContentRoot(buildRoot: Path): ContentRoot = {
    val sourceDirs = Seq(buildRoot)

    val excludedDirs = Seq(
      buildRoot / Sbt.TargetDirectory,
      buildRoot / Sbt.ProjectDirectory / Sbt.TargetDirectory)

    ContentRoot(
      base = buildRoot,
      sources = sourceDirs,
      excluded = excludedDirs)
  }

  def createSbtModuleData(project: sbtStructure.ProjectData, localCachePath: Option[String]): SbtData = {
    val imports = project.build.imports.flatMap(_.trim.substring(7).split(", "))
    val resolvers = project.resolvers.map(r => Resolver(r.name, "maven", r.root)).toSeq
    new SbtData(imports, resolvers)
  }

  private def validRootPathsIn(project: sbtStructure.ProjectData, scope: String)
                              (selector: sbtStructure.ConfigurationData => Seq[sbtStructure.DirectoryData]): Seq[String] = {
    project.configurations
      .find(_.id == scope)
      .map(selector)
      .getOrElse(Seq.empty)
      .map(_.file)
      .filter(!_.isOutsideOf(project.base))
      .map(_.path)
  }

  protected def createLibraryDependencies(dependencies: Seq[sbtStructure.ModuleDependencyData]): Seq[LibraryDependency] = {
    dependencies.map { dependency =>
      val name = nameFor(dependency.id)
      val scope = scopeFor(dependency.configurations)
      new LibraryDependency(name = name, scope = scope)
    }
  }

  private def createUnmanagedDependencies(dependencies: Seq[sbtStructure.JarDependencyData]): Seq[ModuleLevelLibrary] = {
    dependencies.groupBy(it => scopeFor(it.configurations)).toSeq.map { case (scope, dependency) =>
      val name = scope match {
        case Scope.Compile => Sbt.UnmanagedLibraryName
        case it => s"${Sbt.UnmanagedLibraryName}-${displayNameOf(it)}"
      }
      val files = dependency.map(_.file.path)
      createModuleLevelLibrary(name, files, Seq.empty, Seq.empty, scope)
    }
  }

  private def createModuleLevelLibrary(name: String, classes: Seq[String], docs: Seq[String], sources: Seq[String], scope: Scope): ModuleLevelLibrary = {
    new ModuleLevelLibrary(name, scope, classes, sources, docs)
  }

  protected def scopeFor(configurations: Seq[sbtStructure.Configuration]): Scope = {
    val ids = configurations.toSet

    if (ids.contains(sbtStructure.Configuration.Compile))
      Scope.Compile
    else if (ids.contains(sbtStructure.Configuration.Runtime))
      Scope.Runtime
    else if (ids.contains(sbtStructure.Configuration.Test))
      Scope.Test
    else if (ids.contains(sbtStructure.Configuration.Provided))
      Scope.Provided
    else
      Scope.Compile
  }

  private def displayNameOf(scope: Scope): String = scope match {
    case Scope.Compile => "compile"
    case Scope.Runtime => "runtime"
    case Scope.Test => "test"
    case Scope.Provided => "provided"
  }
}
