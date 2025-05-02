import sbt._

object LibDependencies {
  private val httpVerbsVersion = "15.2.0"

  def dependencies(playSuffix: String): Seq[ModuleID] = Seq(
    playOrg(playSuffix)      %% "play-guice"                   % playVersion(playSuffix),
    playOrg(playSuffix)      %% "play-ahc-ws"                  % playVersion(playSuffix),
    "uk.gov.hmrc"            %% s"http-verbs-$playSuffix"      % httpVerbsVersion,

    playOrg(playSuffix)      %% playHttpServer(playSuffix)     % playVersion(playSuffix)              % Test,
    "uk.gov.hmrc"            %% s"http-verbs-test-$playSuffix" % httpVerbsVersion                     % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"           % scalaTestPlusPlayVersion(playSuffix) % Test,
    "org.scalatest"          %% "scalatest"                    % "3.2.18"                             % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"                 % "0.64.8"                             % Test,
    "ch.qos.logback"         %  "logback-classic"              % "1.4.11"                             % Test
  )

  private def playVersion(playSuffix: String) =
    playSuffix match {
      case "play-29" => "2.9.6"
      case "play-30" => "3.0.6"
    }

  private def playOrg(playSuffix: String) =
    playSuffix match {
      case "play-29" => "com.typesafe.play"
      case "play-30" => "org.playframework"
    }

  private def playHttpServer(playSuffix: String) =
    playSuffix match {
      case "play-29" => "play-akka-http-server"
      case "play-30" => "play-pekko-http-server"
    }

  private def scalaTestPlusPlayVersion(playSuffix: String): String =
    playSuffix match {
      case "play-29" => "6.0.1"
      case "play-30" => "7.0.1"
    }
}
