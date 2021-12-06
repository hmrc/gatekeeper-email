import scoverage.ScoverageKeys._

object ScoverageSettings {
  def apply() = Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    coverageExcludedPackages :=
      """uk\.gov\.hmrc\.gatekeeperemail\.domain\.models;""" +
        """uk\.gov\.hmrc\.BuildInfo;""" +
        """.*\.Routes;""" +
        """.*\.RoutesPrefix;""" +
        """.*Filters?;""" +
        """MicroserviceAuditConnector;""" +
        """Module;""" +
        """GraphiteStartUp;""" +
        """.*\.Reverse[^.]*""",
    coverageMinimum := 92.00,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )
}
