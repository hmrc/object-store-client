import sbt.Keys.crossScalaVersions
import sbt._

val name = "object-store-client"

val scala2_11 = "2.11.12"
val scala2_12 = "2.12.10"

lazy val commonResolvers = Seq(
  Resolver.bintrayRepo("hmrc", "releases"),
  Resolver.typesafeRepo("releases")
)

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc.objectstore",
  majorVersion := 0,
  scalaVersion := scala2_12,
  crossScalaVersions := Seq(scala2_11, scala2_12),
  makePublicallyAvailableOnBintray := true,
  resolvers := commonResolvers
)

lazy val library = Project(name, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
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

lazy val objectStoreClientCommon = Project("object-store-client-common", file("object-store-client-common"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.objectStoreClientCommon
  )

lazy val objectStoreClientPlay26 = Project("object-store-client-play-26", file("object-store-client-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.objectStoreClienPlay26
  ).dependsOn(objectStoreClientCommon)

lazy val objectStoreClientPlay27 = Project("object-store-client-play-27", file("object-store-client-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    Compile / unmanagedSourceDirectories  += baseDirectory.value / "../object-store-client-play-26/src/main/scala",
    Test    / unmanagedSourceDirectories  += baseDirectory.value / "../object-store-client-play-26/src/test/scala",
    libraryDependencies ++= AppDependencies.objectStoreClienPlay27
  ).dependsOn(objectStoreClientCommon)
