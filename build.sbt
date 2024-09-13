import uk.gov.hmrc.DefaultBuildSettings

val appName = "gatekeeper-email"

Global / bloopAggregateSourceDependencies := true
Global / bloopExportJarClassifiers := Some(Set("sources"))

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 0
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(ScoverageSettings())
  .settings(
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true
  )
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(
    Test / fork := false,
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon"
  )
  .settings(
    scalacOptions ++= Seq(
      // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
      // suppress warnings in generated routes files
      "-Wconf:src=routes/.*:s"
    )
  )

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    name := "integration-tests",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    DefaultBuildSettings.itSettings()
  )

commands ++= Seq(
  Command.command("cleanAll") { state => "clean" :: "it/clean" :: state },
  Command.command("fmtAll") { state => "scalafmtAll" :: "it/scalafmtAll" :: state },
  Command.command("fixAll") { state => "scalafixAll" :: "it/scalafixAll" :: state },
  Command.command("testAll") { state => "test" :: "it/test" :: state },

  Command.command("run-all-tests") { state => "testAll" :: state },
  Command.command("clean-and-test") { state => "cleanAll" :: "compile" :: "run-all-tests" :: state },
  Command.command("pre-commit") { state => "cleanAll" :: "fmtAll" :: "fixAll" :: "coverage" :: "testAll" :: "coverageOff" :: "coverageAggregate" :: state }
)
