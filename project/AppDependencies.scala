import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.14.0",
    "uk.gov.hmrc"             %% "simple-reactivemongo"       % "8.0.0-play-28",
    "uk.gov.hmrc"             %%  "http-metrics"              % "2.3.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.16.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.56.0",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8",
    "org.mockito"             %%  "mockito-scala-scalatest"   % "1.16.42",
    "uk.gov.hmrc"             %% "reactivemongo-test"       % "5.0.0-play-28",
    "org.scalaj"                  %% "scalaj-http"                        % "2.3.0",
    "com.github.tomakehurst"      %  "wiremock-jre8-standalone"           % "2.27.2"
  )
}
