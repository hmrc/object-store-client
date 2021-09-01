import sbt.Keys.crossScalaVersions
import sbt._

val name      = "object-store-client"

val scala2_12 = "2.12.14"

lazy val commonSettings = Seq(
  organization     := "uk.gov.hmrc.objectstore",
  majorVersion     := 0,
  scalaVersion     := scala2_12,
  isPublicArtefact := true,
)

lazy val library = Project(name, file("."))
  .settings(
    commonSettings,
    publish / skip := true,
    crossScalaVersions := Seq.empty
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
