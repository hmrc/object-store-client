import sbt._

object LibDependencies {
  private val httpVerbsVersion = "14.12.0"

  def dependencies(playSuffix: String): Seq[ModuleID] = Seq(
    playOrg(playSuffix)      %% "play-guice"                   % playVersion(playSuffix),
    playOrg(playSuffix)      %% "play-ahc-ws"                  % playVersion(playSuffix),
    "uk.gov.hmrc"            %% s"http-verbs-$playSuffix"      % httpVerbsVersion,
    playOrg(playSuffix)      %% playHttpServer(playSuffix)     % playVersion(playSuffix)                                % Test,
    "uk.gov.hmrc"            %% s"http-verbs-test-$playSuffix" % httpVerbsVersion                                       % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"           % scalaTestPlusPlayVersion(playSuffix)                   % Test,
    "org.scalatest"          %% "scalatest"                    % "3.2.15"                                               % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"                 % "0.64.8"                                               % Test,
    "ch.qos.logback"         %  "logback-classic"              % logbackClassicVersion(playSuffix)                      % Test
  )

  private def playVersion(playSuffix: String) =
    playSuffix match {
      case "play-28" => "2.8.20"
      case "play-29" => "2.9.0"
      case "play-30" => "3.0.0"
    }

  private def playOrg(playSuffix: String) =
    playSuffix match {
      case "play-28"
         | "play-29" => "com.typesafe.play"
      case "play-30" => "org.playframework"
    }

  private def playHttpServer(playSuffix: String) =
    playSuffix match {
      case "play-28"
         | "play-29" => "play-akka-http-server"
      case "play-30" => "play-pekko-http-server"
    }

  private def scalaTestPlusPlayVersion(playSuffix: String): String =
    playSuffix match {
      case "play-28" => "5.1.0"
      case "play-29" => "6.0.0"
      case "play-30" => "7.0.0"
    }

  private def logbackClassicVersion(playSuffix: String): String =
    playSuffix match {
      case "play-28" => "1.2.12"
      case "play-29"
         | "play-30" => "1.4.11"
    }
}
