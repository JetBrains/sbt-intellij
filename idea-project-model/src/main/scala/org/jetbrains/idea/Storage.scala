package org.jetbrains.idea

import java.io.File

import org.jetbrains.idea.model._

import scala.xml.{Elem, Text, Utility}

/**
 * @author Pavel Fatin
 */
object Storage {
  def write(root: String, project: Project, home: Path) {
    toXml(project, canonical(home)).foreach { case (path, node) =>
      val file = new File(root, path)
      val directory = file.getParentFile
      if (!directory.exists) {
        directory.mkdirs()
      }
      XML.save(file, node)
    }
  }

  def toXml(project: Project, home: Path): Map[Path, Elem] = {
    val index = (".idea/modules.xml", trim(toXml(project.modules.map(_.name))))
    val modules = project.modules.map(path => (".idea/modules" / s"${path.name}.iml", trim(toXml(path, canonical(project.base), home))))
    val libraries = project.libraries.map(path => (".idea/libraries" / s"${escape(path.name)}.xml", trim(toXml(path, home))))
    val scalaSdks = project.scalaSdks.map(path => (".idea/libraries" / s"${escape(path.name)}.xml", trim(toXml(path, home))))
    Map(index +: (modules ++ libraries ++ scalaSdks): _*)
  }

  private def toXml(module: Module, base: Path, home: Path): Elem =
      <module type={format(module.kind)} version="4">
        <component name="NewModuleRootManager" inherit-compiler-output={format(module.outputPaths.isEmpty)} >
          {module.outputPaths.toSeq.flatMap { paths =>
            <output url={"file://$MODULE_DIR$/../.." + relative(base, canonical(paths.production))}/>
            <output-test url={"file://$MODULE_DIR$/../.." + relative(base, canonical(paths.test))}/>
          }}
          {if (module.outputPaths.exists(_.exclude)) {
            <exclude-output />
          }}
          {module.contentRoots.map(it => toXml(it, canonical(base)))}
          <orderEntry type="inheritedJdk" />
          <orderEntry type="sourceFolder" forTests="false" />
          {module.moduleDependencies.map { it =>
            <orderEntry type="module" module-name={it.name} exported={format(it.exported)} />
          }}
          {module.libraryDependencies.map { it =>
            <orderEntry type="library" name={it.name} level={format(it.level)} />
          }}
          {module.libraries.map { library =>
            <orderEntry type="module-library">
              <library name={library.name}>
                <CLASSES>
                  {library.classes.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
                </CLASSES>
                <JAVADOC>
                  {library.docs.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
                </JAVADOC>
                <SOURCES>
                  {library.sources.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
                </SOURCES>
              </library>
            </orderEntry>
          }}
        </component>
      </module>

  private def toXml(root: ContentRoot, base: Path): Elem =
    <content url={"file://$MODULE_DIR$/../.." + relative(base, canonical(root.base))}>
      {root.sources.map(path => <sourceFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(path))} isTestSource="false"/>)}
      {root.testSources.map(path => <sourceFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(path))} isTestSource="true"/>)}
      {root.resources.map(path => <sourceFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(path))} type="java-resource"/>)}
      {root.testResources.map(path => <sourceFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(path))} type="java-test-resource"/>)}
      {root.excluded.map(path => <excludeFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(path))}/>)}
    </content>

  private def toXml(modules: Seq[String]): Elem =
    <project version="4">
      <component name="ProjectModuleManager">
        <modules>
          {modules.map { module =>
            <module fileurl={s"file://$$PROJECT_DIR$$/.idea/modules/$module.iml"} filepath={s"$$PROJECT_DIR$$/.idea/modules/$module.iml"} />
          }}
        </modules>
      </component>
    </project>

  private def toXml(library: Library, home: Path): Elem =
    <component name="libraryTable">
      <library name={library.name}>
        <CLASSES>
          {library.classes.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
        </CLASSES>
        <JAVADOC>
          {library.docs.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
        </JAVADOC>
        <SOURCES>
          {library.sources.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
        </SOURCES>
      </library>
    </component>

  private def toXml(sdk: ScalaSdk, home: Path): Elem =
    <component name="libraryTable">
      <library name={sdk.name} type="Scala">
        <properties>
          <option name="languageLevel" value={sdk.languageLevel} />
          <compiler-classpath>
            {sdk.compilerClasspath.map(path => <root url={"file://$USER_HOME$" + relative(home, canonical(path))}/>)}
          </compiler-classpath>
        </properties>
        <CLASSES>
          {sdk.classes.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
        </CLASSES>
        <JAVADOC>
          {sdk.docs.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
        </JAVADOC>
        <SOURCES>
          {sdk.sources.map(path => <root url={"jar://$USER_HOME$" + relative(home, canonical(path)) + "!/"}/>)}
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

  private def canonical(path: Path): Path = path.replace('\\', '/')

  private def relative(base: Path, path: Path): Path = {
    if (path.startsWith(base)) path.substring(base.length) else path
  }
}
