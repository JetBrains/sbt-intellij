package org.jetbrains.idea

import java.io.File

import org.jetbrains.idea.model._

import scala.xml.{NodeSeq, Elem, Text, Utility}

/**
 * @author Pavel Fatin
 */
object Storage {
  private val ModuleOrLibraryPathPattern = ".idea/(modules|libraries)/".r

  def write(root: File, project: Project, prefix: String, home: Path) {
    toXml(project, prefix, home).foreach { case (path, node) =>
      val file = new File(root, path)
      val directory = file.getParentFile
      if (!directory.exists) {
        directory.mkdirs()
      }
      XML.save(file, node, declaration = ModuleOrLibraryPathPattern.findFirstIn(path).isEmpty)
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
    val settings = (".idea/misc.xml", trim(toXml(project.jdk, project.languageLevel.map(s => "JDK_" + s.replace('.', '_')))))
    val modules = project.modules.map(module => (".idea/modules" / s"${module.name}.iml", trim(toXml(module, prefix, moduleFs))))
    val libraries = project.libraries.map(library => (".idea/libraries" / s"${escape(prefix + library.name)}.xml", trim(toXml(library, prefix, moduleFs))))
    val profiles = (".idea/scala_compiler.xml", trim(toXml(project.profiles)))
    Map(index +: settings +: (modules ++ libraries) :+ profiles: _*)
  }

  private def toXml(jdk: Option[String], languageLevel: Option[JavaLanguageLevel]): Elem =
    <project version="4">
      <component name="ProjectRootManager" version="2" languageLevel={text(languageLevel)} default="false" assert-keyword="true" jdk-15="true" project-jdk-name={text(jdk)} project-jdk-type="JavaSDK">
        <output url="file://$PROJECT_DIR$/classes" />
      </component>
    </project>

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
          {module.jdk.map { jdk =>
            <orderEntry type="jdk" jdkName={jdk} jdkType="JavaSDK"/>
          } getOrElse {
            <orderEntry type="inheritedJdk"/>
          }}
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
      <library name={prefix + library.name} type={optional(library.scalaCompiler.isDefined)("Scala")}>
        {library.scalaCompiler.fold(NodeSeq.Empty) { compiler =>
          <properties>
            <option name="languageLevel" value={"Scala_" + compiler.level.replace('.', '_')} />
            <compiler-classpath>
              {compiler.classpath.map(path => <root url={fs.fileUrlFrom(path)}/>)}
            </compiler-classpath>
          </properties>
        }}
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

  private def toXml(profiles: Seq[Profile]): Elem =
    <project version="4">
      <component name="ScalaCompilerConfiguration">
        {profiles.map { profile =>
          val settings = profile.settings
          <profile name={profile.name} modules={profile.modules.mkString(",")}>
            {if (settings.compileOrder != CompileOrder.Mixed)
              <option name="compileOrder" value={format(settings.compileOrder)} />
            }
            {if (settings.dynamics) <option name="dynamics" value="true" /> }
            {if (settings.postfixOps) <option name="postfixOps" value="true" /> }
            {if (settings.reflectiveCalls) <option name="reflectiveCalls" value="true" /> }
            {if (settings.implicitConversions) <option name="implicitConversions" value="true" /> }
            {if (settings.higherKinds) <option name="higherKinds" value="true" /> }
            {if (settings.existentials) <option name="existentials" value="true" /> }
            {if (settings.macros) <option name="macros" value="true" /> }
            {if (settings.experimental) <option name="experimental" value="true" /> }
            {if (!settings.warnings) <option name="warnings" value="false" /> }
            {if (settings.deprecationWarnings) <option name="deprecationWarnings" value="true" /> }
            {if (settings.uncheckedWarnings) <option name="uncheckedWarnings" value="true" /> }
            {if (settings.featureWarnings) <option name="featureWarnings" value="true" /> }
            {if (settings.optimiseBytecode) <option name="optimiseBytecode" value="true" /> }
            {if (settings.explainTypeErrors) <option name="explainTypeErrors" value="true" /> }
            {if (!settings.specialization) <option name="specialization" value="false" /> }
            {if (settings.continuations) <option name="continuations" value="true" /> }
            {if (settings.debuggingInfoLevel != DebuggingInfoLevel.Vars)
              <option name="debuggingInfoLevel" value={format(settings.debuggingInfoLevel)} />
            }
            {if (settings.additionalCompilerOptions.nonEmpty) {
              <parameters>
                {settings.additionalCompilerOptions.map(it => <parameter value={it}/>)}
              </parameters>
            }}
            {if (settings.plugins.nonEmpty) {
              <plugins>
                {settings.plugins.map(it => <plugin path={it}/>)}
              </plugins>
            }}
          </profile>
        }}
      </component>
    </project>

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

  private def format(order: CompileOrder): String = order match {
    case CompileOrder.Mixed => "Mixed"
    case CompileOrder.JavaThenScala => "JavaThenScala"
    case CompileOrder.ScalaThenJava => "ScalaThenJava"
  }

  private def format(level: DebuggingInfoLevel): String = level match {
    case DebuggingInfoLevel.None => "None"
    case DebuggingInfoLevel.Source => "Source"
    case DebuggingInfoLevel.Line => "Line"
    case DebuggingInfoLevel.Vars => "Vars"
    case DebuggingInfoLevel.Notailcalls => "Notailcalls"
  }

  private def optional(b: Boolean)(s: String): Option[Text] = if (b) Some(Text(s)) else None

  private def trim(elem: Elem): Elem = Utility.trim(elem).asInstanceOf[Elem]

  private def text(v: Option[Any]): Option[Text] = v.map(it => Text(it.toString))

  private def escape(name: String): String = name.map(c => if (c.isLetterOrDigit) c else '_')
}
