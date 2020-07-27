import sbt._

object AppDependencies {

  private val play26Version = "2.6.25"

  val test = Seq(
    "org.mockito"              %  "mockito-all"          % "1.10.19"  % Test,
    "com.github.tomakehurst"   %  "wiremock-standalone"  % "2.27.1"   % Test,
    "org.pegdown"              %  "pegdown"              % "1.6.0"    % Test
  )

  lazy val objectStoreClientCommon: Seq[ModuleID] = Seq(
    "org.slf4j"                       % "slf4j-api" % "1.7.30"
  )

  lazy val objectStoreClienPlay26: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play"                 % play26Version,
    "com.typesafe.play"      %% "play-guice"           % play26Version,
    "com.typesafe.play"      %% "play-ahc-ws"          % play26Version,
    "org.scalatestplus.play" %% "scalatestplus-play"   % "3.1.3"         % Test
  ) ++ test

}
