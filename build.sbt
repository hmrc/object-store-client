val scala2_13 = "2.13.12"
val scala3    = "3.3.4"

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
    objectStoreClientPlay28,
    objectStoreClientPlay29,
    objectStoreClientPlay30
  )

def copyPlay30Sources(module: Project) =
  CopySources.copySources(
    module,
    transformSource   = _.replace("org.apache.pekko", "akka"),
    transformResource = _.replace("pekko", "akka")
  )

lazy val objectStoreClientCommon = Project("object-store-client-common", file("object-store-client-common"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
  )

lazy val objectStoreClientPlay28 = Project("object-store-client-play-28", file("object-store-client-play-28"))
  .settings(
    copyPlay30Sources(objectStoreClientPlay30),
    libraryDependencies ++= LibDependencies.dependencies("play-28")
  )
  .dependsOn(objectStoreClientCommon)

lazy val objectStoreClientPlay29 = Project("object-store-client-play-29", file("object-store-client-play-29"))
  .settings(
    copyPlay30Sources(objectStoreClientPlay30),
    libraryDependencies ++= LibDependencies.dependencies("play-29")
  )
  .dependsOn(objectStoreClientCommon)

lazy val objectStoreClientPlay30 = Project("object-store-client-play-30", file("object-store-client-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.dependencies("play-30")
  )
  .dependsOn(objectStoreClientCommon)
