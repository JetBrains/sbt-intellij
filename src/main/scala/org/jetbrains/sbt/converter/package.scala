package org.jetbrains.sbt

import java.io.File

/**
  * @author Pavel Fatin
  */
package object converter {
  implicit class RichFile(val file: File) extends AnyVal {
//    def /(path: String): File = new File(file, path)

    def `<<`: File = << (1)

    def `<<`(level: Int): File = RichFile.parent(file, level)

    def name: String = file.getName

    def path: String = file.getPath.replaceAll("\\\\", "/")

    def absolutePath: String = file.getAbsolutePath

//    def canonicalPath: String = ExternalSystemApiUtil.toCanonicalPath(file.getAbsolutePath)

//    def canonicalFile: File = new File(canonicalPath)

    def parent: Option[File] = Option(file.getParentFile)

    def endsWith(parts: String*): Boolean = endsWith0(file, parts.reverse)

    private def endsWith0(file: File, parts: Seq[String]): Boolean = if (parts.isEmpty) true else
      parts.head == file.getName && Option(file.getParentFile).exists(endsWith0(_, parts.tail))

////    def url: String = VfsUtil.getUrlForLibraryRoot(file)
//
//    def isAncestorOf(aFile: File): Boolean = FileUtil.isAncestor(file, aFile, true)
//
//    def isUnder(root: File): Boolean = FileUtil.isAncestor(root, file, true)
//
    def isOutsideOf(root: File): Boolean = false//!FileUtil.isAncestor(root, file, false)

    def write(lines: String*) {
//      writeLinesTo(file, lines: _*)
    }

    def copyTo(destination: File) {
//      copy(file, destination)
    }
  }

  private object RichFile {
    def parent(file: File, level: Int): File =
      if (level > 0) parent(file.getParentFile, level - 1) else file
  }
}
