import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "7.12.0"
  lazy val hmrcMongoVersion = "0.74.0"
  lazy val akkaVersion = "2.6.19"

  def apply(): Seq[ModuleID] = compile ++ test

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"   % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"          % hmrcMongoVersion,
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-28" % "1.0.0",
    "com.beachape"            %% "enumeratum-play-json"        % "1.7.0",
    "uk.gov.hmrc"             %% "json-encryption"             % "5.1.0-play-28",
    "org.typelevel"           %% "cats-core"                   % "2.3.1",
    "com.lightbend.akka"      %% "akka-stream-alpakka-mongodb" % "3.0.1",
    "com.typesafe.akka"       %% "akka-stream"                 % akkaVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.7.1",
    "org.scalaj"              %% "scalaj-http"                % "2.4.2",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.27.2"            
  ).map(_ % "test, it")
}
