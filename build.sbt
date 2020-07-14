import sbt._

val appName = "object-store-client"

lazy val library = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.11",
//    makePublicallyAvailableOnBintray := true, // Uncomment if this is a public repository
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
