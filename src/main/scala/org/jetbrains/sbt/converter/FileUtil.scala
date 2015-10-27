package org.jetbrains.sbt.converter

import java.io.File

/**
  * @author Pavel Fatin
  */
object FileUtil {
  def filesEqual(file1: File, file2: File): Boolean = file1.getPath == file2.getPath
}
