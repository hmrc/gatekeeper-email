import sbt.*

object AppDependencies {

  lazy val bootstrapPlayVersion = "10.7.0"
  lazy val hmrcMongoVersion     = "2.12.0"
  lazy val commonDomainVersion  = "1.0.0"
  lazy val apiDomainVersion     = "1.3.0-SNAPSHOT"

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
