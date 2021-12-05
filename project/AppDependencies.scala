import sbt._

object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  val compile = Seq(
    "uk.gov.hmrc"                 %%  "bootstrap-backend-play-28"  % "5.16.0",
    "uk.gov.hmrc.mongo"           %%  "hmrc-mongo-play-28"         % "0.56.0",
    "uk.gov.hmrc"                 %   "http-verbs-play-28_2.12"    % "13.10.0",
    "uk.gov.hmrc"                 %%  "http-metrics"               % "2.3.0-play-28",
    "org.apache.httpcomponents"   %  "httpcore"                    % "4.4.14"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.16.0"            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.56.0"            % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % "test, it",
    "org.mockito"             %%  "mockito-scala-scalatest"   % "1.7.1"             % "test, it"
  )
}
