import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "5.23.0"
  lazy val hmrcMongoVersion = "0.63.0"
  def apply(): Seq[ModuleID] = compile ++ test

  val compile = Seq(
    "uk.gov.hmrc"                 %%  "bootstrap-backend-play-28"   % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"           %%  "hmrc-mongo-play-28"          % hmrcMongoVersion,
    "uk.gov.hmrc.objectstore"     %%  "object-store-client-play-28" % "0.39.0",
    "com.beachape" %% "enumeratum-play-json" % "1.7.0"


  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapPlayVersion % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion     % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"             % "test, it",
    "org.mockito"             %%  "mockito-scala-scalatest"   % "1.7.1"              % "test, it",
    "org.scalaj"              %% "scalaj-http"                % "2.3.0"              % "test, it",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.27.2"             % "test, it"
  )
}
