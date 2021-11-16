import sbt._

object Resolvers {
  def apply() = Seq(
    Resolver.typesafeRepo("releases")
  )
}
