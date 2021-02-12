import sbt.Keys.crossScalaVersions
import sbt._

val name      = "object-store-client"

val scala2_12 = "2.12.10"

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc.objectstore",
  majorVersion := 0,
  scalaVersion := scala2_12,
  makePublicallyAvailableOnBintray := true,
)

lazy val library = Project(name, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .settings(
    commonSettings,
    publish := {},
    publishAndDistribute := {},
    crossScalaVersions := Seq.empty
  )
  .aggregate(
    objectStoreClientCommon,
    objectStoreClientPlay26,
    objectStoreClientPlay27
  )

def copySources(module: Project) = Seq(
  Compile / scalaSource := (module / Compile / scalaSource).value,
  Compile / resources   := (module / Compile / resources  ).value,
  Test    / scalaSource := (module / Test    / scalaSource).value,
  Test    / resources   := (module / Test    / resources  ).value
)

lazy val objectStoreClientCommon = Project("object-store-client-common", file("object-store-client-common"))
  .enablePlugins(SbtAutoBuildPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.objectStoreClientCommon
  )

lazy val objectStoreClientPlay26 = Project("object-store-client-play-26", file("object-store-client-play-26"))
  .enablePlugins(SbtAutoBuildPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.objectStoreClientPlay26
  )
  .dependsOn(objectStoreClientCommon)

lazy val objectStoreClientPlay27 = Project("object-store-client-play-27", file("object-store-client-play-27"))
  .enablePlugins(SbtAutoBuildPlugin)
  .settings(
    commonSettings,
    copySources(objectStoreClientPlay26),
    libraryDependencies ++= AppDependencies.objectStoreClientPlay27
  )
  .dependsOn(objectStoreClientCommon)

lazy val objectStoreClientPlay28 = Project("object-store-client-play-28", file("object-store-client-play-28"))
  .enablePlugins(SbtAutoBuildPlugin)
  .settings(
    commonSettings,
    copySources(objectStoreClientPlay26),
    libraryDependencies ++= AppDependencies.objectStoreClientPlay28
  )
  .dependsOn(objectStoreClientCommon)
