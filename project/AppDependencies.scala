import sbt._

object AppDependencies {

  val test = Seq(
    "org.mockito"              %  "mockito-all"          % "1.10.19"  % Test,
    "com.github.tomakehurst"   %  "wiremock-standalone"  % "2.27.1"   % Test,
    "org.pegdown"              %  "pegdown"              % "1.6.0"    % Test
  )

  lazy val objectStoreClientCommon: Seq[ModuleID] = Seq(
    "org.slf4j"                       % "slf4j-api" % "1.7.30"
  )

}
