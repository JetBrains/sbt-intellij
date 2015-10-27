package org.jetbrains.idea

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}

import scala.xml.{Node, PrettyPrinter}

/**
  * @author Pavel Fatin
  */
private object XML {
  private val Encoding = "UTF-8"

  // PrettyPrinter relies on hardcoded '\n' line separator - exactly what is needed,
  // because IDEA always uses this line separator in config files, regardless of OS.
  private val printer = new PrettyPrinter(180, 2)

  private val Header = s"""<?xml version="1.0" encoding="$Encoding"?>"""

  // Improvement over scala.xml.XML.save (to add proper formatting)
  def save(file: File, node: Node) {
    write(file, Encoding) { writer =>
      writer.write(Header)
      writer.write('\n')
      writer.write(printer.format(node))
    }
  }

  private def write(file: File, encoding: String)(f: BufferedWriter => Unit) {
    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding))
    try {
      f(writer)
    } finally {
      writer.close()
    }
  }
}
