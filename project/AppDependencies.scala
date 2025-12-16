import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "10.5.0"
  lazy val hmrcMongoVersion     = "2.11.0"
  lazy val commonDomainVersion  = "0.19.0"
  lazy val apiDomainVersion     = "0.20.0"

  def apply(): Seq[ModuleID] = compile ++ test

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"   % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"          % hmrcMongoVersion,
    "org.typelevel"           %% "cats-core"                   % "2.13.0",
    "uk.gov.hmrc"             %% "api-platform-api-domain"     % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-30"              % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-30"             % hmrcMongoVersion,
    "uk.gov.hmrc"           %% "api-platform-common-domain-fixtures" % commonDomainVersion
  ).map(_ % "test")
}
