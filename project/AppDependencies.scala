import sbt._

object AppDependencies {
  lazy val objectStoreClientPlay28: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play"                    % play28Version,
    "com.typesafe.play"      %% "play-guice"              % play28Version,
    "com.typesafe.play"      %% "play-ahc-ws"             % play28Version,
    "com.typesafe.play"      %% "play-akka-http-server"   % play28Version    % Test,
    "uk.gov.hmrc"            %% "http-verbs-play-28"      % httpVerbsVersion,
    "uk.gov.hmrc"            %% "http-verbs-test-play-28" % httpVerbsVersion % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"          % Test,
    "org.scalatest"          %% "scalatest"               % "3.2.3"          % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.35.10"        % Test,
    "ch.qos.logback"         %  "logback-classic"         % "1.2.3"          % Test
  )
  private val play28Version    = "2.8.16"
  private val httpVerbsVersion = "14.7.0"
}
