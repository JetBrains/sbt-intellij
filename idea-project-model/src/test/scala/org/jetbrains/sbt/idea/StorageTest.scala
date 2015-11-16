package org.jetbrains.sbt.idea

import org.jetbrains.idea.Storage
import org.jetbrains.idea.model._
import org.junit.{Assert, Test}

import scala.xml.Utility._
import scala.xml.{Elem, PrettyPrinter}

/**
  * @author Pavel Fatin
  */
class StorageTest {
  @Test
  def basic(): Unit = {
    val project =
      Project("name", "base",
        modules = Seq(
          Module("id",
            contentRoots = Seq(
              ContentRoot("base",
                sources = Seq("base/src/main/scala"),
                resources = Seq("base/src/main/resources"),
                testSources = Seq("base/src/test/scala"),
                testResources = Seq("base/src/test/resources"),
                excluded = Seq("base/target")))),
          Module("id-build", ModuleKind.Sbt,
            outputPaths = Some(OutputPaths(
              "base/project/target/idea-classes",
              "base/project/target/idea-test-classes")),
            contentRoots = Seq(
              ContentRoot("base/project",
                sources = Seq("base/project"),
                excluded = Seq("base/project/target", "base/project/project/target"))),
            libraries = Seq(
              ModuleLevelLibrary("sbt-and-plugins",
                classes = Seq("home/sbt.jar"),
                sources = Seq("home/sbt-sources.jar"),
                docs = Seq("home/sbt-javadoc.jar"))),
            moduleDependencies = Seq(
              ModuleDependency("id")),
            libraryDependencies = Seq(
              LibraryDependency("junit")))),
        libraries = Seq(
          Library("junit",
            classes = Seq("home/junit.jar"),
            sources = Seq("home/junit-sources.jar"),
            docs = Seq("home/junit-javadoc.jar"))
        ),
        scalaSdks = Seq(
          ScalaSdk("scala-sdk", "2.11",
            classes = Seq("home/scala-library.jar"),
            sources = Seq("home/scala-library-sources.jar"),
            docs = Seq("home/scala-library-javadoc.jar"),
            compilerClasspath = Seq("home/scala-compiler.jar", "home/scala-library.jar"))
        )
      )

    assertSerialized(project,
      Map(
        ".idea/modules.xml" ->
          <project version="4">
            <component name="ProjectModuleManager">
              <modules>
                <module fileurl="file://$PROJECT_DIR$/.idea/modules/id.iml" filepath="$PROJECT_DIR$/.idea/modules/id.iml"/>
                <module fileurl="file://$PROJECT_DIR$/.idea/modules/id-build.iml" filepath="$PROJECT_DIR$/.idea/modules/id-build.iml"/>
              </modules>
            </component>
          </project>,

        ".idea/modules/id.iml" ->
          <module type="JAVA_MODULE" version="4">
            <component name="NewModuleRootManager" inherit-compiler-output="true">
              <content url="file://$MODULE_DIR$/../..">
                <sourceFolder url="file://$MODULE_DIR$/../../src/main/scala" isTestSource="false" />
                <sourceFolder url="file://$MODULE_DIR$/../../src/test/scala" isTestSource="true" />
                <sourceFolder url="file://$MODULE_DIR$/../../src/main/resources" type="java-resource" />
                <sourceFolder url="file://$MODULE_DIR$/../../src/test/resources" type="java-test-resource" />
                <excludeFolder url="file://$MODULE_DIR$/../../target"/>
              </content>
              <orderEntry type="inheritedJdk"/>
              <orderEntry type="sourceFolder" forTests="false"/>
            </component>
          </module>,

        ".idea/modules/id-build.iml" ->
          <module type="SBT_MODULE" version="4">
            <component name="NewModuleRootManager" inherit-compiler-output="false">
              <output url="file://$MODULE_DIR$/../../project/target/idea-classes"/>
              <output-test url="file://$MODULE_DIR$/../../project/target/idea-test-classes"/>
              <exclude-output/>
              <content url="file://$MODULE_DIR$/../../project">
                <sourceFolder url="file://$MODULE_DIR$/../../project" isTestSource="false"/>
                <excludeFolder url="file://$MODULE_DIR$/../../project/target"/>
                <excludeFolder url="file://$MODULE_DIR$/../../project/project/target"/>
              </content>
              <orderEntry type="inheritedJdk"/>
              <orderEntry type="sourceFolder" forTests="false"/>
              <orderEntry type="module" module-name="id" exported="true"/>
              <orderEntry type="library" name="junit" level="project"/>
              <orderEntry type="module-library">
                <library name="sbt-and-plugins">
                  <CLASSES>
                    <root url="jar://$USER_HOME$/sbt.jar!/"/>
                  </CLASSES>
                  <JAVADOC>
                    <root url="jar://$USER_HOME$/sbt-javadoc.jar!/"/>
                  </JAVADOC>
                  <SOURCES>
                    <root url="jar://$USER_HOME$/sbt-sources.jar!/"/>
                  </SOURCES>
                </library>
              </orderEntry>
            </component>
          </module>,

        ".idea/libraries/junit.xml" ->
          <component name="libraryTable">
            <library name="junit">
              <CLASSES>
                <root url="jar://$USER_HOME$/junit.jar!/"/>
              </CLASSES>
              <JAVADOC>
                <root url="jar://$USER_HOME$/junit-javadoc.jar!/"/>
              </JAVADOC>
              <SOURCES>
                <root url="jar://$USER_HOME$/junit-sources.jar!/"/>
              </SOURCES>
            </library>
          </component>,

        ".idea/libraries/scala_sdk.xml" ->
          <component name="libraryTable">
            <library name="scala-sdk" type="Scala">
              <properties>
                <option name="languageLevel" value="2.11"/>
                <compiler-classpath>
                  <root url="file://$USER_HOME$/scala-compiler.jar"/>
                  <root url="file://$USER_HOME$/scala-library.jar"/>
                </compiler-classpath>
              </properties>
              <CLASSES>
                <root url="jar://$USER_HOME$/scala-library.jar!/"/>
              </CLASSES>
              <JAVADOC>
                <root url="jar://$USER_HOME$/scala-library-javadoc.jar!/"/>
              </JAVADOC>
              <SOURCES>
                <root url="jar://$USER_HOME$/scala-library-sources.jar!/"/>
              </SOURCES>
            </library>
          </component>
      ))
  }

  private def assertSerialized(project: Project, expected: Map[Path, Elem]): Unit = {
    val actual = Storage.toXml(project, "home")

    if (expected.map(p => (p._1, trim(p._2))) != actual) {
      val printer = new PrettyPrinter(120, 2)

      def format(pairs: Map[Path, Elem]): String =
        pairs.toSeq.sortBy(_._1).map(p => p._1 + ":\n" + printer.format(p._2)).mkString("\n\n")

      Assert.assertEquals(format(expected), format(actual))
      Assert.fail("XML representations are equal, but values are not")
    }
  }
}
