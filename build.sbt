val scala2_12 = "2.12.17"
val scala2_13 = "2.13.10"

lazy val commonSettings = Seq(
  organization       := "uk.gov.hmrc.objectstore",
  majorVersion       := 1,
  isPublicArtefact   := true,
  scalaVersion       := scala2_12,
  crossScalaVersions := Seq(scala2_12, scala2_13),
  scalacOptions      ++= Seq("-feature")
)

lazy val library = Project("object-store-client", file("."))
  .settings(
    commonSettings,
    publish / skip := true
  )
  .aggregate(
    objectStoreClientCommon,
    objectStoreClientPlay28
  )

lazy val objectStoreClientCommon = Project("object-store-client-common", file("object-store-client-common"))
  .settings(
    commonSettings
  )

lazy val objectStoreClientPlay28 = Project("object-store-client-play-28", file("object-store-client-play-28"))
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.objectStoreClientPlay28
  )
  .dependsOn(objectStoreClientCommon)
