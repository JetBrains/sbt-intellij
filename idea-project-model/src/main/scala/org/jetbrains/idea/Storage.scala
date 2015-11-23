package org.jetbrains.idea

import java.io.File

import org.jetbrains.idea.model._

import scala.xml.{Elem, Text, Utility}

/**
 * @author Pavel Fatin
 */
object Storage {
  def write(root: File, project: Project, prefix: String, home: Path) {
    toXml(project, prefix, home).foreach { case (path, node) =>
      val file = new File(root, path)
      val directory = file.getParentFile
      if (!directory.exists) {
        directory.mkdirs()
      }
      XML.save(file, node)
    }
  }

  def toXml(project: Project, prefix: String, home: Path): Map[Path, Elem] = {
    val projectFs = FileSystem.Instance
      .withAlias(home, "$USER_HOME$")
      .withAlias(project.base, "$PROJECT_DIR$")

    val moduleDirectory = s"${project.base}/.idea/modules"

    val moduleFs = projectFs
      .withAlias(project.base, "$MODULE_DIR$/../..")

    val index = (".idea/modules.xml", trim(toXml(project.modules.map(_.name), moduleDirectory, projectFs)))
    val modules = project.modules.map(module => (".idea/modules" / s"${module.name}.iml", trim(toXml(module, prefix, moduleFs))))
    val libraries = project.libraries.map(library => (".idea/libraries" / s"${escape(library.name)}.xml", trim(toXml(library, prefix, moduleFs))))
    val scalaSdks = project.scalaSdks.map(sdk => (".idea/libraries" / s"${escape(sdk.name)}.xml", trim(toXml(sdk, prefix, moduleFs))))
    Map(index +: (modules ++ libraries ++ scalaSdks): _*)
  }

  private def toXml(module: Module, prefix: String, fs: FileSystem): Elem =
      <module type={format(module.kind)} version="4"
              sbt.imports={module.sbtData.map(_.imports.mkString(", ")).map(Text(_))}
              sbt.resolvers={module.sbtData.map(_.resolvers.map(format).mkString(", ")).map(Text(_))}>
        <component name="NewModuleRootManager" inherit-compiler-output={format(module.outputPaths.isEmpty)} >
          {module.outputPaths.toSeq.flatMap { paths =>
            <output url={fs.fileUrlFrom(paths.production)}/>
            <output-test url={fs.fileUrlFrom(paths.test)}/>
          }}
          {if (module.outputPaths.exists(_.exclude)) {
            <exclude-output />
          }}
          {module.contentRoots.map(it => toXml(it, fs))}
          <orderEntry type="inheritedJdk" />
          <orderEntry type="sourceFolder" forTests="false" />
          {module.moduleDependencies.map { it =>
            <orderEntry type="module" module-name={it.name} exported={format(it.exported)} />
          }}
          {module.libraryDependencies.map { it =>
            <orderEntry type="library" name={prefix + it.name} level={format(it.level)} />
          }}
          {module.libraries.map { library =>
            <orderEntry type="module-library">
              <library name={prefix + library.name}>
                <CLASSES>
                  {library.classes.map(path => <root url={fs.urlFrom(path)}/>)}
                </CLASSES>
                <JAVADOC>
                  {library.docs.map(path => <root url={fs.urlFrom(path)}/>)}
                </JAVADOC>
                <SOURCES>
                  {library.sources.map(path => <root url={fs.urlFrom(path)}/>)}
                </SOURCES>
              </library>
            </orderEntry>
          }}
        </component>
      </module>

  private def format(resolver: Resolver): String = s"${resolver.root}|${resolver.kind}|${resolver.name}"

  private def toXml(root: ContentRoot, fs: FileSystem): Elem =
    <content url={fs.fileUrlFrom(root.base)}>
      {root.sources.map(path => <sourceFolder url={fs.fileUrlFrom(path)} isTestSource="false"/>)}
      {root.testSources.map(path => <sourceFolder url={fs.fileUrlFrom(path)} isTestSource="true"/>)}
      {root.resources.map(path => <sourceFolder url={fs.fileUrlFrom(path)} type="java-resource"/>)}
      {root.testResources.map(path => <sourceFolder url={fs.fileUrlFrom(path)} type="java-test-resource"/>)}
      {root.excluded.map(path => <excludeFolder url={fs.fileUrlFrom(path)}/>)}
    </content>

  private def toXml(modules: Seq[String], directory: Path, fs: FileSystem): Elem =
    <project version="4">
      <component name="ProjectModuleManager">
        <modules>
          {modules.map { module =>
            val moduleFilePath: Path = s"$directory/$module.iml"
            <module fileurl={fs.urlFrom(moduleFilePath)} filepath={fs.pathFrom(moduleFilePath)} />
          }}
        </modules>
      </component>
    </project>

  private def toXml(library: Library, prefix: String, fs: FileSystem): Elem =
    <component name="libraryTable">
      <library name={prefix + library.name}>
        <CLASSES>
          {library.classes.map(path => <root url={fs.urlFrom(path)}/>)}
        </CLASSES>
        <JAVADOC>
          {library.docs.map(path => <root url={fs.urlFrom(path)}/>)}
        </JAVADOC>
        <SOURCES>
          {library.sources.map(path => <root url={fs.urlFrom(path)}/>)}
        </SOURCES>
      </library>
    </component>

  private def toXml(sdk: ScalaSdk, prefix: String, fs: FileSystem): Elem =
    <component name="libraryTable">
      <library name={prefix + sdk.name} type="Scala">
        <properties>
          <option name="languageLevel" value={sdk.languageLevel} />
          <compiler-classpath>
            {sdk.compilerClasspath.map(path => <root url={fs.fileUrlFrom(path)}/>)}
          </compiler-classpath>
        </properties>
        <CLASSES>
          {sdk.classes.map(path => <root url={fs.urlFrom(path)}/>)}
        </CLASSES>
        <JAVADOC>
          {sdk.docs.map(path => <root url={fs.urlFrom(path)}/>)}
        </JAVADOC>
        <SOURCES>
          {sdk.sources.map(path => <root url={fs.urlFrom(path)}/>)}
        </SOURCES>
      </library>
    </component>

  private def format(b: Boolean): String = b.toString

  private def format(kind: ModuleKind): String = kind match {
    case ModuleKind.Java => "JAVA_MODULE"
    case ModuleKind.Sbt => "SBT_MODULE"
    case ModuleKind.Source => "SOURCE_MODULE"
  }

  private def format(level: LibraryLevel): String = level match {
    case LibraryLevel.Application => "application"
    case LibraryLevel.Project => "project"
    case LibraryLevel.Module => "module"
  }

  private def optional(b: Boolean)(s: String): Option[Text] = if (b) Some(Text(s)) else None

  private def trim(elem: Elem): Elem = Utility.trim(elem).asInstanceOf[Elem]

  private def escape(name: String) = name.map(c => if (c.isLetterOrDigit) c else '_')
}
