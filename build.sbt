import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import sbt.Keys.baseDirectory
import sbt.Test
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.{DefaultBuildSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName = "gatekeeper-email"

lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val ComponentTest = config("component") extend Test

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

inThisBuild(
  List(
    scalaVersion := "2.12.15",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtDistributablesPlugin
  )
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(playPublishingSettings: _*)
  .settings(ScoverageSettings())
  .settings(defaultSettings(): _*)
  .settings(
    name := appName,
    scalaVersion := "2.12.15",
    libraryDependencies ++= AppDependencies(),
    majorVersion := 0,
    resolvers ++= Resolvers(),
    routesImport += "uk.gov.hmrc.gatekeeperemail.controllers.binders._"
  )
  .settings(SilencerSettings())
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(
    Test / fork := false,
    Test / parallelExecution := false,
    Test / unmanagedSourceDirectories += baseDirectory.value / "test" / "common",
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon"
  )
  .configs(IntegrationTest)
  .settings(DefaultBuildSettings.integrationTestSettings())
  .settings(
    IntegrationTest / fork := false,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "it",
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest(
      (IntegrationTest / definedTests).value
    ),
    addTestReportOption(IntegrationTest, "int-test-reports")
  )
  .configs(ComponentTest)
  .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
  .settings(
    ComponentTest / unmanagedSourceDirectories += baseDirectory.value / "component",
    ComponentTest / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    ComponentTest / testGrouping := oneForkedJvmPerTest((ComponentTest / definedTests).value),
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)


lazy val playPublishingSettings: Seq[sbt.Setting[_]] = Seq(
  Compile / packageDoc / publishArtifact  := false,
  Compile / packageSrc / publishArtifact  := false
)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    Group(
      test.name,
      Seq(test),
      SubProcess(
        ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))
      )
    )
  }
