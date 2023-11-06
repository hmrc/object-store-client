val scala2_12 = "2.12.18"
val scala2_13 = "2.13.12"

// Disable multiple project tests running at the same time
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

ThisBuild / organization       := "uk.gov.hmrc.objectstore"
ThisBuild / majorVersion       := 1
ThisBuild / isPublicArtefact   := true
ThisBuild / scalaVersion       := scala2_13
ThisBuild / scalacOptions      ++= Seq("-feature")

lazy val library = Project("object-store-client", file("."))
  .settings(publish / skip := true)
  .aggregate(
    objectStoreClientCommon,
    objectStoreClientPlay28,
    objectStoreClientPlay29
  )

def copySources(module: Project) = Seq(
  Compile / scalaSource       := (module / Compile / scalaSource      ).value,
  Compile / resourceDirectory := (module / Compile / resourceDirectory).value,
  Test    / scalaSource       := (module / Test    / scalaSource      ).value,
  Test    / resourceDirectory := (module / Test    / resourceDirectory).value
)

lazy val objectStoreClientCommon = Project("object-store-client-common", file("object-store-client-common"))
  .settings(
    crossScalaVersions := Seq(scala2_12, scala2_13)
  )

lazy val objectStoreClientPlay28 = Project("object-store-client-play-28", file("object-store-client-play-28"))
  .settings(
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= LibDependencies.dependencies("play-28")
  )
  .dependsOn(objectStoreClientCommon)

lazy val objectStoreClientPlay29 = Project("object-store-client-play-29", file("object-store-client-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    copySources(objectStoreClientPlay28),
    libraryDependencies ++= LibDependencies.dependencies("play-29")
  )
  .dependsOn(objectStoreClientCommon)
