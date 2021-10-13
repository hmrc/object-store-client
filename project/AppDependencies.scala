import sbt._

object AppDependencies {
  lazy val objectStoreClientCommon: Seq[ModuleID] = Seq(
    "org.slf4j"         %  "slf4j-api" % "1.7.30"
  )
  lazy val objectStoreClientPlay26: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play"                  % play26Version,
    "com.typesafe.play"      %% "play-guice"            % play26Version,
    "com.typesafe.play"      %% "play-ahc-ws"           % play26Version,
    "uk.gov.hmrc"            %% "http-verbs-play-26"    % httpVerbsVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"    % "3.1.3"        % Test,
    "org.pegdown"            %  "pegdown"               % "1.6.0"        % Test
  ) ++ test
  lazy val objectStoreClientPlay27: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play"                  % play27Version,
    "com.typesafe.play"      %% "play-guice"            % play27Version,
    "com.typesafe.play"      %% "play-ahc-ws"           % play27Version,
    "uk.gov.hmrc"            %% "http-verbs-play-27"    % httpVerbsVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"    % "4.0.3"        % Test,
    "org.pegdown"            %  "pegdown"               % "1.6.0"        % Test
  ) ++ test
  lazy val objectStoreClientPlay28: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play"                  % play28Version,
    "com.typesafe.play"      %% "play-guice"            % play28Version,
    "com.typesafe.play"      %% "play-ahc-ws"           % play28Version,
    "uk.gov.hmrc"            %% "http-verbs-play-28"    % httpVerbsVersion,
    "com.typesafe.play"      %% "play-akka-http-server" % "2.8.7"        % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"    % "5.1.0"        % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"          % "0.35.10"      % Test
  ) ++ test
  val test = Seq(
    "org.mockito"            % "mockito-all"            % "1.10.19"      % Test,
    "com.github.tomakehurst" % "wiremock-standalone"    % "2.27.2"       % Test,
    "ch.qos.logback"         % "logback-classic"        % "1.2.3"        % Test
  )
  private val play26Version    = "2.6.25"
  private val play27Version    = "2.7.4"
  private val play28Version    = "2.8.8"
  private val httpVerbsVersion = "13.10.0"
}
