package org.jetbrains.idea

import java.io.File

import org.jetbrains.idea.model._

import scala.xml.{Elem, Text, Utility}

/**
 * @author Pavel Fatin
 */
object Storage {
  def write(root: String, project: Project) {
    toXml(project).foreach { case (path, node) =>
      val file = new File(root, path)
      val directory = file.getParentFile
      if (!directory.exists) {
        directory.mkdirs()
      }
      XML.save(file, node)
    }
  }

  def toXml(project: Project): Map[Path, Elem] = {
    val index = (".idea/modules.xml", trim(toXml(project.modules.map(_.name))))
    val modules = project.modules.map(it => (".idea/modules" / s"${it.name}.iml", trim(toXml(it, canonical(project.base)))))
    val libraries = project.libraries.map(it => (".idea/libraries" / s"${escape(it.name)}.xml", trim(toXml(it))))
    val scalaSdks = project.scalaSdks.map(it => (".idea/libraries" / s"${escape(it.name)}.xml", trim(toXml(it))))
    Map(index +: (modules ++ libraries ++ scalaSdks): _*)
  }

  private def toXml(module: Module, base: Path): Elem =
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
        </component>
      </module>

  private def toXml(root: ContentRoot, base: Path): Elem =
    <content url={"file://$MODULE_DIR$/../.." + relative(base, canonical(root.base))}>
      {root.sources.map(it => <sourceFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(it))} isTestSource="false"/>)}
      {root.testSources.map(it => <sourceFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(it))} isTestSource="true"/>)}
      {root.resources.map(it => <sourceFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(it))} type="java-resource"/>)}
      {root.testResources.map(it => <sourceFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(it))} type="java-test-resource"/>)}
      {root.excluded.map(it => <excludeFolder url={"file://$MODULE_DIR$/../.." + relative(base, canonical(it))}/>)}
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

  private def toXml(library: Library): Elem =
    <component name="libraryTable">
      <library name={library.name}>
        <CLASSES>
          {library.classes.map(url => <root url={url + "!/"}/>)}
        </CLASSES>
        <JAVADOC>
          {library.docs.map(url => <root url={url + "!/"}/>)}
        </JAVADOC>
        <SOURCES>
          {library.sources.map(url => <root url={url + "!/"}/>)}
        </SOURCES>
      </library>
    </component>

  private def toXml(sdk: ScalaSdk): Elem =
    <component name="libraryTable">
      <library name={sdk.name} type="Scala">
        <properties>
          <option name="languageLevel" value={sdk.languageLevel} />
          <compiler-classpath>
            {sdk.compilerClasspath.map(url => <root url={url}/>)}
          </compiler-classpath>
        </properties>
        <CLASSES>
          {sdk.classes.map(url => <root url={url}/>)}
        </CLASSES>
        <JAVADOC>
          {sdk.docs.map(url => <root url={url}/>)}
        </JAVADOC>
        <SOURCES>
          {sdk.sources.map(url => <root url={url}/>)}
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
