name := "sttp-play-root"
organization in ThisBuild := "com.leadiq"
organizationName in ThisBuild := "LeadIQ"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.6"

// don't publish the surrounding multi-project build
publish := {}
publishLocal := {}

val ScalaVersion = "2.12.6"
val CrossScalaVersion = Seq("2.11.8")


def commonProject(id: String): Project = {
  Project(id, file(id)).settings(
    name := id,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-deprecation:false",
      "-feature",
      "-Ywarn-dead-code",
      "-encoding", "UTF-8"
    ),

    resolvers ++= Seq(
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
    ),

    // don't publish the test code as an artifact
    publishArtifact in Test := false,

    // disable publishing empty ScalaDocs
    publishArtifact in (Compile, packageDoc) := false
  )
}

def sttpPlayJson(includePlayVersion: String, scalaVersion: String): Project = {
  val (playSuffix, crossScalaVersion) = includePlayVersion match {
    case Dependencies.play23Version => ("23", CrossScalaVersion)
    case Dependencies.play25Version => ("25", CrossScalaVersion)
    case Dependencies.play26Version => ("26", Seq.empty)
  }

  val id = s"sttp-play$playSuffix-json"
  commonProject(id).settings(
    sourceDirectory := baseDirectory.value / "sttp-play23-json" / "src",
    crossScalaVersions := crossScalaVersion,
    libraryDependencies ++= Seq(
      Dependencies.sttpCore,
      Dependencies.playJson(includePlayVersion, scalaVersion)
    )
  )
}

def sttpPlayBackend(includePlayVersion: String, scalaVersion: String): Project = {
  val playSuffix = includePlayVersion match {
    case Dependencies.play23Version => "23"
  }

  val id = s"sttp-play$playSuffix-backend"
  commonProject(id).settings(
    crossScalaVersions := CrossScalaVersion,
    libraryDependencies ++= Seq(
      Dependencies.sttpCore,
      Dependencies.playWS(includePlayVersion, scalaVersion)
    )
  )
}

lazy val `sttp-play23-json` = sttpPlayJson(Dependencies.play23Version, "2.11")
lazy val `sttp-play25-json` = sttpPlayJson(Dependencies.play25Version, "2.11")
lazy val `sttp-play26-json` = sttpPlayJson(Dependencies.play26Version, "2.12")
lazy val `sttp-play23-backend` = sttpPlayBackend(Dependencies.play23Version, "2.11")
