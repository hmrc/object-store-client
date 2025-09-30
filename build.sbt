val scala2_13 = "2.13.16"
val scala3    = "3.3.5"

// Disable multiple project tests running at the same time
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

ThisBuild / organization       := "uk.gov.hmrc.objectstore"
ThisBuild / majorVersion       := 2
ThisBuild / isPublicArtefact   := true
ThisBuild / scalaVersion       := scala2_13
ThisBuild / scalacOptions      ++= Seq("-feature")

lazy val library = Project("object-store-client", file("."))
  .settings(publish / skip := true)
  .aggregate(
    objectStoreClientCommon,
    objectStoreClientPlay30
  )

lazy val objectStoreClientCommon = Project("object-store-client-common", file("object-store-client-common"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
  )

lazy val objectStoreClientPlay30 = Project("object-store-client-play-30", file("object-store-client-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.dependencies("play-30")
  )
  .dependsOn(objectStoreClientCommon)
