import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "7.12.0"
  lazy val hmrcMongoVersion     = "1.7.0"
  lazy val akkaVersion          = "2.6.19"
  lazy val commonDomainVersion  = "0.10.0"
  lazy val apiDomainVersion     = "0.11.0"

  def apply(): Seq[ModuleID] = compile ++ test

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"   % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"          % hmrcMongoVersion,
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-28" % "1.0.0",
    "org.typelevel"           %% "cats-core"                   % "2.10.0",
    "com.lightbend.akka"      %% "akka-stream-alpakka-mongodb" % "3.0.1",
    "com.typesafe.akka"       %% "akka-stream"                 % akkaVersion,
    "uk.gov.hmrc"             %% "api-platform-api-domain"     % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-28"          % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-28"         % hmrcMongoVersion,
    "org.mockito"           %% "mockito-scala-scalatest"         % "1.17.29",
    "org.scalatest"         %% "scalatest"                       % "3.2.17",
    "com.vladsch.flexmark"   % "flexmark-all"                    % "0.62.2",
    "org.scalaj"            %% "scalaj-http"                     % "2.4.2",
    "com.github.tomakehurst" % "wiremock-jre8-standalone"        % "2.35.0",
    "uk.gov.hmrc"           %% "api-platform-test-common-domain" % commonDomainVersion
  ).map(_ % "test, it")
}
