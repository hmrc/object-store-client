import sbt._

object LibDependencies {
  private val httpVerbsVersion = "14.11.0"

  private def playVersion(playSuffix: String) =
    playSuffix match {
      case "play-28" => "2.8.20"
      case "play-29" => "2.9.0"
    }

  def dependencies(playSuffix: String): Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play"                         % playVersion(playSuffix),
    "com.typesafe.play"      %% "play-guice"                   % playVersion(playSuffix),
    "com.typesafe.play"      %% "play-ahc-ws"                  % playVersion(playSuffix),
    "uk.gov.hmrc"            %% s"http-verbs-$playSuffix"      % httpVerbsVersion,
    "com.typesafe.play"      %% "play-akka-http-server"        % playVersion(playSuffix)                                % Test,
    "uk.gov.hmrc"            %% s"http-verbs-test-$playSuffix" % httpVerbsVersion                                       % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"           % (if (playSuffix == "play-29") "6.0.0"   else "5.1.0" ) % Test,
    "org.scalatest"          %% "scalatest"                    % "3.2.15"                                               % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"                 % "0.64.8"                                               % Test,
    "ch.qos.logback"         %  "logback-classic"              % (if (playSuffix == "play-29") "1.4.11"  else "1.2.12") % Test
  )
}
