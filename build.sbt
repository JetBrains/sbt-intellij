lazy val sbtProjectModel =
  (project in file("sbt-project-model"))
    .settings(sbtPlugin := true)

lazy val ideaProjectModel =
  (project in file("idea-project-model"))
    .settings(libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test)

lazy val launcher =
  (project in file("launcher"))
    .settings(autoScalaLibrary := false)

lazy val root =
  (project in file(".")).
    dependsOn(ideaProjectModel, sbtProjectModel, launcher % Runtime)
    .settings(
      name := "sbt-intellij",
      version := "1.0",
      sbtPlugin := true,
      libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test//,
//      libraryDependencies += "org.scala-sbt" % "sbt-launch" % "0.13.6" % Runtime
    )

mainClass in assembly := Some("org.jetbrains.sbt.Launcher")

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

assemblyMergeStrategy in assembly := {
  case PathList("sbt", "sbt.plugins", xs @ _*) => MergeStrategy.discard
  case x =>
    val delegate = (assemblyMergeStrategy in assembly).value
    delegate(x)
}
