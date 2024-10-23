import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "9.4.0"
  lazy val hmrcMongoVersion     = "2.2.0"
  lazy val commonDomainVersion  = "0.17.0"
  lazy val apiDomainVersion     = "0.19.1"

  def apply(): Seq[ModuleID] = compile ++ test

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"   % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"          % hmrcMongoVersion,
    "org.typelevel"           %% "cats-core"                   % "2.10.0",
    "org.apache.pekko"        %% "pekko-connectors-mongodb"    % "1.0.2",
    "uk.gov.hmrc"             %% "api-platform-api-domain"     % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-30"              % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-30"             % hmrcMongoVersion,
    "org.scalatestplus"     %% "mockito-5-10"                        % "3.2.18.0",
    "uk.gov.hmrc"           %% "api-platform-common-domain-fixtures" % commonDomainVersion
  ).map(_ % "test")
}
