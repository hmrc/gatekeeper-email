import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import sbt.Keys.baseDirectory
import sbt.Test
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import bloop.integrations.sbt.BloopDefaults

val appName = "gatekeeper-email"

lazy val playSettings: Seq[Setting[_]] = Seq.empty

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 0
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(playPublishingSettings: _*)
  .settings(ScoverageSettings())
  .settings(defaultSettings(): _*)
  .settings(
    name := appName,
    libraryDependencies ++= AppDependencies(),
    resolvers ++= Resolvers(),
    routesImport += "uk.gov.hmrc.gatekeeperemail.controllers.binders._"
  )
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(
    Test / fork := false,
    Test / parallelExecution := false,
    Test / unmanagedSourceDirectories += baseDirectory.value / "test" / "common",
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon"
  )
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:cat=unused&src=views/.*\\.scala:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    )
  )


lazy val playPublishingSettings: Seq[sbt.Setting[_]] = Seq(
  Compile / packageDoc / publishArtifact  := false,
  Compile / packageSrc / publishArtifact  := false
)

Global / bloopAggregateSourceDependencies := true

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    name := "integration-tests",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    DefaultBuildSettings.itSettings()
  )

commands ++= Seq(
  Command.command("cleanAll") { state => "clean" :: "it/clean" :: state},
  Command.command("fmtAll") { state => "scalafmtAll" :: "it/scalafmtAll" :: state},
  Command.command("fixAll") { state => "scalafixAll" :: "it/scalafixAll" :: state},
  Command.command("testAll") { state => "test" :: "it/test" :: state},
  Command.command("run-all-tests") { state => "testAll" :: state },
  Command.command("clean-and-test") { state => "cleanAll" :: "compile" :: "run-all-tests" :: state },
  Command.command("pre-commit") { state => "cleanAll" :: "fmtAll" :: "fixAll" :: "coverage" :: "run-all-tests" :: "coverageOff" :: "coverageAggregate" :: state }
)
