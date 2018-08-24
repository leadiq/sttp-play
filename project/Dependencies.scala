import sbt._

object Dependencies {

  val play23Version = "2.3.10"
  val play24Version = "2.4.11"
  val play25Version = "2.5.10"
  val play26Version = "2.6.7"

  def playJson(playVersion: String): ModuleID = {
    "com.typesafe.play" %% s"play-json" % playVersion
  }

  def playWS(playVersion: String): ModuleID = {
    "com.typesafe.play" %% s"play-ws" % playVersion % "provided"
  }

  val sttpVersion = "1.1.10"

  val sttpCore = "com.softwaremill.sttp" %% "core" % sttpVersion

  val testDependencies = "org.scalatest" %% "scalatest" % "3.0.5" % Test
}
