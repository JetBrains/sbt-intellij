package org.jetbrains.idea.model

/**
  * @author Pavel Fatin
  */
case class JavaCompilerSettings(targetBytecodeLevel: Option[String] = None,
                                moduleTargetBytecodeLevel: Map[String, String] = Map.empty,
                                additionalCompilerOptions: Seq[String] = Seq.empty)