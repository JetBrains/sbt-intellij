package org.jetbrains.sbt.converter

import java.io.File

import org.jetbrains.idea.Serializer
import org.jetbrains.idea.model._
import org.jetbrains.sbt.structure._
import org.junit.{Assert, Test}

import scala.xml.PrettyPrinter

/**
  * @author Pavel Fatin
  */
class ConversionTest {
  @Test
  def basic(): Unit = {
    val sbtStructure =
      StructureData("0.13.5",
        projects = Seq(
          ProjectData("id", "name", "organization", "1.0",
            base = file("base"),
            target = file("base/target"),
            scala = Some(
              ScalaData(version = "2.11.7",
                file("scala-library-2.11.7.jar"),
                file("scala-compiler-2.11.7.jar"),
                Seq(file("scala-reflect-2.11.7.jar")),
                Seq("-optimise"))),
            dependencies = DependencyData(
              modules = Seq(
                ModuleDependencyData(
                  ModuleIdentifier("org.scala-lang", "scala-library", "2.11.7", "jar", ""),
                  Seq(Configuration("compile"))),
                ModuleDependencyData(
                  ModuleIdentifier("org.scala-lang", "scala-xml", "2.11.7", "jar", ""),
                  Seq(Configuration("compile"))))))),
        repository = Some(
          RepositoryData(
            modules = Seq(
              ModuleData(
                ModuleIdentifier("org.scala-lang", "scala-library", "2.11.7", "jar", ""),
                Set(file("scala-library-2.11.7.jar")),
                Set(file("scala-library-2.11.7-javadoc.jar")),
                Set(file("scala-library-2.11.7-sources.jar")))))),
        localCachePath = None)

    val ideaProject =
      Project("name", "base",
        jdk = Some("1.6"),
        modules = Seq(
          Module("id",
            contentRoots = Seq(
              ContentRoot("base",
                excluded = Seq("base/target"))),
            libraryDependencies = Seq(
              LibraryDependency("org.scala-lang:scala-library:2.11.7:jar"),
              LibraryDependency("org.scala-lang:scala-xml:2.11.7:jar"))),
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
            sbtData = Some(SbtData()))),
        libraries = Seq(
          Library("org.scala-lang:scala-library:2.11.7:jar",
            Seq("scala-library-2.11.7.jar"),
            Seq("scala-library-2.11.7-sources.jar"),
            Seq("scala-library-2.11.7-javadoc.jar"),
            scalaCompiler = Some(
              ScalaCompiler("2.11",
                Seq("scala-library-2.11.7.jar",
                  "scala-compiler-2.11.7.jar",
                  "scala-reflect-2.11.7.jar")))),
          Library("org.scala-lang:scala-xml:2.11.7:jar", resolved = false)),
        profiles = Seq(
          Profile("SBT 1", ScalaCompilerSettings(optimiseBytecode = true), Seq("id"))
        ))

    assertConvertedTo(sbtStructure, ideaProject)
  }

  private def assertConvertedTo(structure: StructureData, expected: Project): Unit = {
    val actual = Converter.convert("base", structure, Some("1.6"))

    if (expected != actual) {
      val printer = new PrettyPrinter(120, 2)

      def format(project: Project): String = printer.format(Serializer.toXml(project))

      Assert.assertEquals(format(expected), format(actual))
      Assert.fail("XML representations are equal, but values are not")
    }
  }

  private def file(path: String): File = new File(path)
}
