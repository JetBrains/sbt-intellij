package org.jetbrains.idea

import org.jetbrains.idea.model.Path

/**
  * @author Pavel Fatin
  */
class FileSystem private (aliases: List[(String, Path)]) {
  def withAlias(path: Path, alias: String): FileSystem = {
    new FileSystem((alias, canonical(path)) :: aliases)
  }

  def urlFrom(path: Path): String =
    if (path.endsWith(".jar")) jarUrlFrom(path) else fileUrlFrom(path)

  def fileUrlFrom(path: Path): String =
    "file://" + pathFrom(canonical(path))

  def jarUrlFrom(path: Path): String =
    "jar://" + pathFrom(canonical(path)) + "!/"

  def pathFrom(path: Path): Path = {
    aliases.foldLeft(path) { case (result, (alias, prefix)) =>
      if (result.startsWith(prefix)) alias + result.substring(prefix.length) else result
    }
  }

  def canonical(path: Path): Path =
    path.replace('\\', '/')
}

object FileSystem {
  val Instance = new FileSystem(List.empty)
}