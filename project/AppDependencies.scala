import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "8.4.0"
  lazy val hmrcMongoVersion     = "1.7.0"
  lazy val commonDomainVersion  = "0.11.0"
  lazy val apiDomainVersion     = "0.13.0"

  def apply(): Seq[ModuleID] = compile ++ test

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"   % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"          % hmrcMongoVersion,
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-30" % "1.3.0",
    "org.typelevel"           %% "cats-core"                   % "2.10.0",
    "org.apache.pekko"        %% "pekko-connectors-mongodb"    % "1.0.2",
    "uk.gov.hmrc"             %% "api-platform-api-domain"     % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-30"          % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-30"         % hmrcMongoVersion,
    "org.mockito"           %% "mockito-scala-scalatest"         % "1.17.29",
    "org.scalaj"            %% "scalaj-http"                     % "2.4.2",
    "uk.gov.hmrc"           %% "api-platform-test-common-domain" % commonDomainVersion
  ).map(_ % "test, it")
}
