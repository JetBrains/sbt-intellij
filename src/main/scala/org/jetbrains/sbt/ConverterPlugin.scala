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
object ConverterPlugin extends Plugin with (State => State) {
  private val DefaultJdkVersion = Some("1.6")

  def convert(state: State) {
    val log = state.log

    val currentDirectory = System.getProperty("user.dir")

    log.info("Reading SBT project structure from " + currentDirectory + "...")

    val options = Options(download = true, resolveClassifiers = false,
      resolveSbtClassifiers = false, cachedUpdate = false)

    val structure = StructureExtractor.apply(state, options)

    log.info("Converting project to IDEA model...")
    val project = Converter.convert(currentDirectory, structure, systemJdkVersion.orElse(DefaultJdkVersion))

    log.info("Writing IDEA project definition...")
    Storage.write(new File(currentDirectory), project, "SBT: ", System.getProperty("user.home"))

    log.info("Done.")
  }

  def read(state: State) {
    val log = state.log

    val currentDirectory = System.getProperty("user.dir")

    log.info("Reading SBT project structure from " + currentDirectory + "...")
    val structure = StructureExtractor.apply(state, Options.readFromString(""))

    log.info("Converting project to IDEA model...")
    val project = Converter.convert(currentDirectory, structure, systemJdkVersion.orElse(DefaultJdkVersion))

    val text = trim(Serializer.toXml(project)).mkString

    intellijOutputPath.in(Project.current(state)).get(Project.extract(state).structure.data) match {
      case Some(path) =>
        log.info("Writing structure to " + path + "...")
        write(new File(path), text)
        log.info("Done.")
      case None =>
        log.info("Writing structure to console:")
        println(text)
        log.info("Done.")
    }
  }

  private def systemJdkVersion: Option[String] = Option(System.getProperty("java.version")).map(_.substring(0, 3))

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

  override lazy val settings: Seq[Setting[_]] =
    Seq(intellijOutputPath := "project.xml", commands += intellijReadCommand, commands += intellijConvertCommand)

  lazy val intellijReadCommand = Command.command("intellij-read") { state => read(state); state}

  lazy val intellijConvertCommand = Command.command("intellij-convert") { state => convert(state); state}

  lazy val intellijOutputPath = settingKey[String]("Project structure XML output path for 'intellij-read' command")

  def apply(state: State): State = state.copy(definedCommands =
    state.definedCommands :+ intellijReadCommand :+ intellijConvertCommand)
}
