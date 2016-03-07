package org.jetbrains.idea

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}

import scala.xml.{Elem, NamespaceBinding, Node, PrettyPrinter}

/**
  * @author Pavel Fatin
  */
private object XML {
  private val Encoding = "UTF-8"

  // PrettyPrinter relies on hardcoded '\n' line separator - exactly what is needed,
  // because IDEA always uses this line separator in config files, regardless of OS.
  private val printer = new PrettyPrinter(1024, 2) {
    // Add trailing space in leaf elements
    // (on a par with IDEA, to make file comparison easier).
    override protected def leafTag(n: Node) = {
      val builder = new StringBuilder()
      builder.append('<')
      n.nameToString(builder)
      n.attributes.buildString(builder)
      builder.append(" />")
      builder.toString
    }

    // https://stackoverflow.com/questions/6044883/scala-xml-prettyprinter-to-format-shorter-node-when-there-is-no-text-in-it
    override protected def traverse(node: Node, pscope: NamespaceBinding, ind: Int){
      node match {
        case n: Elem if n.child.isEmpty => makeBox(ind, leafTag(n))
        case _ => super.traverse(node, pscope, ind)
      }
    }
  }

  private val Header = s"""<?xml version="1.0" encoding="$Encoding"?>"""

  // Improvement over scala.xml.XML.save (to add proper formatting)
  def save(file: File, node: Node, declaration: Boolean) {
    write(file, Encoding) { writer =>
      if (declaration) {
        writer.write(Header)
        writer.write('\n')
      }
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
