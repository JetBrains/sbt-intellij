package org.jetbrains.idea.model

import org.jetbrains.idea.model.CompileOrder.Mixed
import org.jetbrains.idea.model.DebuggingInfoLevel.Vars

/**
  * @author Pavel Fatin
  */
case class ScalaCompilerSettings(
  compileOrder: CompileOrder = Mixed,

  dynamics: Boolean = false,
  postfixOps: Boolean = false,
  reflectiveCalls: Boolean = false,
  implicitConversions: Boolean = false,
  higherKinds: Boolean = false,
  existentials: Boolean = false,
  macros: Boolean = false,
  experimental: Boolean = false,

  warnings: Boolean = true,
  deprecationWarnings: Boolean = false,
  uncheckedWarnings: Boolean = false,
  featureWarnings: Boolean = false,
  optimiseBytecode: Boolean = false,
  explainTypeErrors: Boolean = false,
  specialization: Boolean = true,
  continuations: Boolean = false,

  debuggingInfoLevel: DebuggingInfoLevel = Vars,
  additionalCompilerOptions: Seq[String] = Seq.empty,
  plugins: Seq[String] = Seq.empty) {
}

object ScalaCompilerSettings {
  private val BasicOptions: Set[String] = Set(
    "-language:dynamics",
    "-language:postfixOps",
    "-language:reflectiveCalls",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:macros",
    "-Xexperimental",
    "-nowarn",
    "-deprecation",
    "-unchecked",
    "-feature",
    "-optimise",
    "-explaintypes",
    "-no-specialization",
    "-P:continuations:enable")

  private val DebuggingOptions: Map[String, DebuggingInfoLevel] = Map(
    "-g:none" -> DebuggingInfoLevel.None,
    "-g:source" -> DebuggingInfoLevel.Source,
    "-g:line" -> DebuggingInfoLevel.Line,
    "-g:vars" -> DebuggingInfoLevel.Vars,
    "-g:notailcalls" -> DebuggingInfoLevel.Notailcalls)

  private val PluginOptionPattern = "-Xplugin:(.+)".r

  def apply(options: Seq[String]): ScalaCompilerSettings = {
    apply(normalized(options).toSet)
  }

  private def apply(options: Set[String]): ScalaCompilerSettings = {
    val debuggingInfoLevel = DebuggingOptions.find(p => options.contains(p._1)).map(_._2).getOrElse(DebuggingInfoLevel.Vars)

    val plugins = options.collect {
      case PluginOptionPattern(path) => path
    }

    val additionalCompilerOptions = (options -- BasicOptions -- DebuggingOptions.keySet)
      .filterNot(option => PluginOptionPattern.findFirstIn(option).isDefined)

    ScalaCompilerSettings(
      dynamics = options.contains("-language:dynamics"),
      postfixOps = options.contains("-language:postfixOps"),
      reflectiveCalls = options.contains("-language:reflectiveCalls"),
      implicitConversions = options.contains("-language:implicitConversions"),
      higherKinds = options.contains("-language:higherKinds"),
      existentials = options.contains("-language:existentials"),
      macros = options.contains("-language:macros"),
      experimental = options.contains("-Xexperimental"),
      warnings = !options.contains("-nowarn"),
      deprecationWarnings = options.contains("-deprecation"),
      uncheckedWarnings = options.contains("-unchecked"),
      featureWarnings = options.contains("-feature"),
      optimiseBytecode = options.contains("-optimise"),
      explainTypeErrors = options.contains("-explaintypes"),
      specialization = !options.contains("-no-specialization"),
      continuations = options.contains("-P:continuations:enable"),
      debuggingInfoLevel = debuggingInfoLevel,
      additionalCompilerOptions = additionalCompilerOptions.toSeq,
      plugins = plugins.toSeq
    )
  }

  private def normalized(options: Seq[String]): Seq[String] = options.flatMap { option =>
    if (option.startsWith("-language:")) {
      option.substring(10).split(",").map("-language:" + _)
    } else {
      Seq(option)
    }
  }
}