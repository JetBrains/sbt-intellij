package org.jetbrains.idea

import java.io.File

/**
  * @author Pavel Fatin
  */
package object model {
  type Path = String

  implicit class RichString(val s: String) extends AnyVal {
    def /(path: Path): Path = s + "/" + path
  }

  implicit class RichFile(val file: File) extends AnyVal {
    def /(path: Path): Path = file.getPath + "/" + path
  }
}
