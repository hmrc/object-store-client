import sbt._

object LibDependencies {
  private val httpVerbsVersion = "15.6.0"

  def dependencies(playSuffix: String): Seq[ModuleID] = Seq(
    "org.playframework"      %% "play-guice"                   % playVersion(playSuffix),
    "org.playframework"      %% "play-ahc-ws"                  % playVersion(playSuffix),
    "uk.gov.hmrc"            %% s"http-verbs-$playSuffix"      % httpVerbsVersion,

    "org.playframework"      %% "play-pekko-http-server"       % playVersion(playSuffix)              % Test,
    "uk.gov.hmrc"            %% s"http-verbs-test-$playSuffix" % httpVerbsVersion                     % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"           % scalaTestPlusPlayVersion(playSuffix) % Test,
    "org.scalatest"          %% "scalatest"                    % "3.2.18"                             % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"                 % "0.64.8"                             % Test,
    "ch.qos.logback"         %  "logback-classic"              % "1.4.11"                             % Test
  )

  private def playVersion(playSuffix: String) =
    playSuffix match {
      case "play-30" => "3.0.9"
    }

  private def scalaTestPlusPlayVersion(playSuffix: String): String =
    playSuffix match {
      case "play-30" => "7.0.1"
    }
}
