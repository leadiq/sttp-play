import sbt._

object Dependencies {

  val play23Version = "2.3.10"
  val play25Version = "2.5.10"
  val play26Version = "2.6.7"

  def playJson(playVersion: String, scalaVersion: String): ModuleID = {
    "com.typesafe.play" % s"play-json_$scalaVersion" % playVersion
  }

  def playWS(playVersion: String, scalaVersion: String): ModuleID = {
    "com.typesafe.play" % s"play-ws_$scalaVersion" % playVersion % "provided"
  }

  val sttpVersion = "1.1.10"

  val sttpCore = "com.softwaremill.sttp" %% "core" % sttpVersion

}
