import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, integrationTestSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import sbt.Keys.baseDirectory
import sbt.Test
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName = "gatekeeper-email"

lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val ComponentTest = config("component") extend Test

lazy val microservice = (project in file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtAutoBuildPlugin,
    SbtDistributablesPlugin
  )
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(ScoverageSettings())
  .settings(defaultSettings(): _*)
  .settings(
    name := appName,
    targetJvm := "jvm-1.8",
    scalaVersion := "2.12.11",
    libraryDependencies ++= AppDependencies(),
    majorVersion := 0,
    resolvers ++= Resolvers(),
    routesImport += "uk.gov.hmrc.gatekeeperemail.controllers.binders._"
  )
  .settings(SilencerSettings())
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(
    unmanagedResourceDirectories in IntegrationTest += baseDirectory.value / "test" / "resources"
  )
  .settings(
    unmanagedResourceDirectories in Compile += baseDirectory.value / "app" / "resources"
  )
  .settings(scalacOptions ++= Seq("-Ypartial-unification"))
