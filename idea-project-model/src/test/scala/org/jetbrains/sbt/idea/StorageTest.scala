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
              ModuleLevelLibrary("sbt-and-plugins")))),
        libraries = Seq(
          Library("junit",
            classes = Seq("junit.jar"),
            sources = Seq("junit-src.jar"),
            docs = Seq("junit-doc.jar"))
        ),
        scalaSdks = Seq(
          ScalaSdk("scala-sdk", "2.11",
            classes = Seq("scala-library.jar"),
            sources = Seq("scala-library-src.jar"),
            docs = Seq("scala-library-doc.jar"),
            compilerClasspath = Seq("scala-compiler.jar", "scala-library.jar"))
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

        ".idea/modules/id.xml" ->
          <module type="JAVA_MODULE" version="4">
            <component name="NewModuleRootManager" inherit-compiler-output="true">
              <content url="file://$MODULE_DIR$/../../idea-project-model">
                <excludeFolder url="file://$MODULE_DIR$/../../base/target"/>
              </content>
              <orderEntry type="inheritedJdk"/>
              <orderEntry type="sourceFolder" forTests="false"/>
            </component>
          </module>,

        ".idea/modules/id-build.xml" ->
          <module type="SBT_MODULE" version="4">
            <component name="NewModuleRootManager" inherit-compiler-output="false">
              <output url="file://$MODULE_DIR$/../../base/project/target/idea-classes"/>
              <output-test url="file://$MODULE_DIR$/../../base/project/target/idea-test-classes"/>
              <exclude-output/>
              <content url="file://$MODULE_DIR$/../../idea-project-model">
                <sourceFolder url="file://$MODULE_DIR$/../../base/project" isTestSource="false"/>
                <excludeFolder url="file://$MODULE_DIR$/../../base/project/target"/>
                <excludeFolder url="file://$MODULE_DIR$/../../base/project/project/target"/>
              </content>
              <orderEntry type="inheritedJdk"/>
              <orderEntry type="sourceFolder" forTests="false"/>
            </component>
          </module>,

        ".idea/libraries/junit.xml" ->
          <component name="libraryTable">
            <library name="junit">
              <CLASSES>
                <root url="junit.jar!/"/>
              </CLASSES>
              <JAVADOC>
                <root url="junit-doc.jar!/"/>
              </JAVADOC>
              <SOURCES>
                <root url="junit-src.jar!/"/>
              </SOURCES>
            </library>
          </component>,

        ".idea/libraries/scala_sdk.xml" ->
          <component name="libraryTable">
            <library name="scala-sdk" type="Scala">
              <properties>
                <option name="languageLevel" value="2.11"/>
                <compiler-classpath>
                  <root url="scala-compiler.jar"/>
                  <root url="scala-library.jar"/>
                </compiler-classpath>
              </properties>
              <CLASSES>
                <root url="scala-library.jar"/>
              </CLASSES>
              <JAVADOC>
                <root url="scala-library-doc.jar"/>
              </JAVADOC>
              <SOURCES>
                <root url="scala-library-src.jar"/>
              </SOURCES>
            </library>
          </component>
      ))
  }

  private def assertSerialized(project: Project, expected: Map[Path, Elem]): Unit = {
    val actual = Storage.toXml(project)

    if (expected.map(p => (p._1, trim(p._2))) != actual) {
      val printer = new PrettyPrinter(120, 2)

      def format(pairs: Map[Path, Elem]): String =
        pairs.toSeq.sortBy(_._1).map(p => p._1 + ":\n" + printer.format(p._2)).mkString("\n\n")

      Assert.assertEquals(format(expected), format(actual))
      Assert.fail("XML representations are equal, but values are not")
    }
  }
}
