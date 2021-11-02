import sbt._

// Disable multiple project tests running at the same time - for free port discovery
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

lazy val commonSettings = Seq(
  organization     := "uk.gov.hmrc.objectstore",
  majorVersion     := 0,
  scalaVersion     := "2.12.14",
  isPublicArtefact := true,
)

lazy val library = Project("object-store-client", file("."))
  .settings(
    commonSettings,
    publish / skip := true
  )
  .aggregate(
    objectStoreClientCommon,
    objectStoreClientPlay26,
    objectStoreClientPlay27,
    objectStoreClientPlay28
  )

def copySources(module: Project) = Seq(
  Compile / scalaSource       := (module / Compile / scalaSource      ).value,
  Compile / resourceDirectory := (module / Compile / resourceDirectory).value,
  Test    / scalaSource       := (module / Test    / scalaSource      ).value,
  Test    / resourceDirectory := (module / Test    / resourceDirectory).value
)

lazy val objectStoreClientCommon = Project("object-store-client-common", file("object-store-client-common"))
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.objectStoreClientCommon
  )

lazy val objectStoreClientPlay26 = Project("object-store-client-play-26", file("object-store-client-play-26"))
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.objectStoreClientPlay26
  )
  .dependsOn(objectStoreClientCommon)

lazy val objectStoreClientPlay27 = Project("object-store-client-play-27", file("object-store-client-play-27"))
  .settings(
    commonSettings,
    copySources(objectStoreClientPlay26),
    libraryDependencies ++= AppDependencies.objectStoreClientPlay27
  )
  .dependsOn(objectStoreClientCommon)

lazy val objectStoreClientPlay28 = Project("object-store-client-play-28", file("object-store-client-play-28"))
  .settings(
    commonSettings,
    copySources(objectStoreClientPlay26),
    libraryDependencies ++= AppDependencies.objectStoreClientPlay28
  )
  .dependsOn(objectStoreClientCommon)
