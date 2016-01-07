package org.jetbrains.sbt.idea

import org.jetbrains.idea.Serializer
import org.jetbrains.idea.model.LibraryLevel.Application
import org.jetbrains.idea.model.Scope.Runtime
import org.jetbrains.idea.model._
import org.junit.{Assert, Test}

import scala.xml.PrettyPrinter

/**
  * @author Pavel Fatin
  */
class SerializationTest {
  @Test
  def empty(): Unit = {
    assertSerializedAndDeserizlizedCorrectly(Project("name", "base"))
  }

  @Test
  def basic(): Unit = {
    val project =
      Project("name", "base",
        jdk = Some("1.6"),
        languageLevel = Some("1.5"),
        modules = Seq(
          Module("id",
            jdk = Some("1.8"),
            languageLevel = Some("1.7"),
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
              ModuleLevelLibrary("sbt-and-plugins")),
            moduleDependencies = Seq(
              ModuleDependency(
                name = "id",
                scope = Runtime,
                exported = false)),
            libraryDependencies = Seq(
              LibraryDependency(
                name = "scalatest",
                level = Application,
                scope = Scope.Test)),
            sbtData = Some(SbtData(
              imports = Seq("org.foo.bar"),
              resolvers = Seq(Resolver("name", "kind", "root")))))),
        libraries = Seq(
          Library("junit",
            classes = Seq("junit.jar"),
            sources = Seq("junit-src.jar"),
            docs = Seq("junit-doc.jar")),
          Library("scala-sdk",
            classes = Seq("scala-library.jar"),
            sources = Seq("scala-library-src.jar"),
            docs = Seq("scala-library-doc.jar"),
            scalaCompiler = Some(ScalaCompiler(
              level = "2.11",
              classpath = Seq("scala-compiler.jar", "scala-library.jar"))))
        ),
        profiles = Seq(
          Profile("Profile 1",
            ScalaCompilerSettings(
              dynamics = true,
              additionalCompilerOptions = Seq("-target:jvm-1.6"),
              plugins = Seq("continuations.jar")),
            Seq("id")))
        )

    assertSerializedAndDeserizlizedCorrectly(project)
  }

  private def assertSerializedAndDeserizlizedCorrectly(project: Project): Unit = {
    val xml = Serializer.toXml(project)

    val loadedProject = Serializer.fromXml(xml)

    if (project != loadedProject) {
      val printer = new PrettyPrinter(180, 2)
      val expected = printer.format(xml)
      val actual = printer.format(Serializer.toXml(loadedProject))
      Assert.assertEquals(expected, actual)
      Assert.fail("Serialized and de-serialized projects differ")
    }
  }
}
