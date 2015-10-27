lazy val sbtProjectModel = (project in file("sbt-project-model"))
  .settings(sbtPlugin := true, scalaVersion := "2.10.5")

lazy val ideaProjectModel = (project in file("idea-project-model"))
  .settings(
    scalaVersion := "2.10.5",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
  )

lazy val root = (project in file(".")).
  dependsOn(ideaProjectModel, sbtProjectModel).
  settings(
          name := "sbt-intellij",
          version := "1.0",
          sbtPlugin := true,
          scalaVersion := "2.10.5",
          libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
  )

