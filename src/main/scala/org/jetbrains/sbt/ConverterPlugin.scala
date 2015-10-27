package org.jetbrains.sbt

import java.io.{BufferedWriter, FileOutputStream, OutputStreamWriter}

import org.jetbrains.idea.{Serializer, Storage}
import org.jetbrains.sbt.converter.Converter
import org.jetbrains.sbt.extractors.StructureExtractor
import sbt.Keys._
import sbt._

import scala.xml.Utility.trim

/**
  * @author Pavel Fatin
  */
object ConverterPlugin extends Plugin {
  def convert(state: State) {
    val log = state.log

    val currentDirectory = System.getProperty("user.dir")

    log.info("Reading SBT project structure from " + currentDirectory + "...")
    val structure = StructureExtractor.apply(state, Options.readFromString(""))

    log.info("Converting project to IDEA model...")
    val project = Converter.convert(currentDirectory, structure, None)

    log.info("Writing IDEA project definition...")
    Storage.write(currentDirectory, project)

    log.info("Done.")
  }

  def read(state: State) {
    val log = state.log

    val currentDirectory = System.getProperty("user.dir")

    log.info("Reading SBT project structure from " + currentDirectory + "...")
    val structure = StructureExtractor.apply(state, Options.readFromString(""))

    log.info("Converting project to IDEA model...")
    val project = Converter.convert(currentDirectory, structure, None)

    val text = trim(Serializer.toXml(project)).mkString

    Keys.artifactPath.in(Project.current(state)).get(Project.extract(state).structure.data) match {
      case Some(file) =>
        log.info("Writing structure to " + file.getPath + "...")
        write(file, text)
        log.info("Done.")
      case None =>
        log.info("Writing structure to console:")
        println(text)
        log.info("Done.")
    }
  }

  private def write(file: File, xml: String) {
    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
    try {
      writer.write( """<?xml version="1.0" encoding="UTF-8"?>""")
      writer.newLine()
      writer.write(xml)
      writer.flush()
    } finally {
      writer.close()
    }
  }

  override lazy val settings: Seq[Setting[_]] = Seq(commands += convertProjectCommand)

  lazy val readProjectCommand = Command.command("read-intellij")((s: State) => ConvertProject(s))

  lazy val convertProjectCommand = Command.command("gen-intellij")((s: State) => ConvertProject(s))
}

object ReadProjectModel extends (State => State) {
  def apply(state: State) = Function.const(state)(ConverterPlugin.read(state))
}

object ConvertProject extends (State => State) {
  def apply(state: State) = Function.const(state)(ConverterPlugin.convert(state))
}

