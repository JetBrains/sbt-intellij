package org.jetbrains.sbt.converter

import java.io.File

import org.jetbrains.idea.Serializer
import org.jetbrains.idea.model._
import org.jetbrains.sbt.structure.{ProjectData, StructureData}
import org.junit.{Assert, Test}

import scala.xml.PrettyPrinter

/**
  * @author Pavel Fatin
  */
class ConversionTest {
  @Test
  def empty(): Unit = {
    val sbtProject = ProjectData("id", "name", "organization", "1.0",
      new File("base"), target = new File("base/target"))

    val ideaProject =
      Project("name", "base", Seq(
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
            ModuleLevelLibrary("sbt-and-plugins")),
          sbtData = Some(SbtData()))))

    assertConvertedTo(sbtProject, ideaProject)
  }

  private def assertConvertedTo(projectData: ProjectData, expected: Project): Unit = {
    val structure = StructureData("0.13.5", Seq(projectData), None, None)

    val actual = Converter.convert("base", structure, None)

    if (expected != actual) {
      val printer = new PrettyPrinter(120, 2)

      def format(project: Project): String = printer.format(Serializer.toXml(project))

      Assert.assertEquals(format(expected), format(actual))
      Assert.fail("XML representations are equal, but values are not")
    }
  }
}
