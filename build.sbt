name := "sttp-play-root"
organization in ThisBuild := "com.leadiq"
organizationName in ThisBuild := "LeadIQ"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.6"

// don't publish the surrounding multi-project build
publish := {}
publishLocal := {}

val Scala212Version = "2.12.6"
val Scala211Version = "2.11.12"
val CrossScalaVersion = Seq("2.12.6", "2.11.12")

def commonProject(id: String, ScalaVersion: String): Project = {
  Project(id, file(id)).settings(
    name := id,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-deprecation:false",
      "-feature",
      "-Ywarn-dead-code",
      "-encoding", "UTF-8",
      "-language:higherKinds"
    ),

    scalaVersion := ScalaVersion,

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
    case Dependencies.play23Version => ("23", Seq(scalaVersion))
    case Dependencies.play24Version => ("24", Seq(scalaVersion))
    case Dependencies.play25Version => ("25", Seq(scalaVersion))
    case Dependencies.play26Version => ("26", CrossScalaVersion)
  }

  val id = s"sttp-play$playSuffix-json"
  commonProject(id, scalaVersion).settings(
    sourceDirectory := baseDirectory.value / ".." / "sttp-play23-json" / "src",
    crossScalaVersions := crossScalaVersion,
    libraryDependencies ++= Seq(
      Dependencies.sttpCore,
      Dependencies.playJson(includePlayVersion),
      Dependencies.testDependencies
    )
  )
}

def sttpPlayBackend(includePlayVersion: String, scalaVersion: String): Project = {
  val playSuffix = includePlayVersion match {
    case Dependencies.play23Version => "23"
  }

  val id = s"sttp-play$playSuffix-backend"
  commonProject(id, scalaVersion).settings(
    crossScalaVersions := Seq(scalaVersion),
    libraryDependencies ++= Seq(
      Dependencies.sttpCore,
      Dependencies.playWS(includePlayVersion)
    )
  )
}

lazy val `sttp-play23-json` = sttpPlayJson(Dependencies.play23Version, Scala211Version)
lazy val `sttp-play24-json` = sttpPlayJson(Dependencies.play24Version, Scala211Version)
lazy val `sttp-play25-json` = sttpPlayJson(Dependencies.play25Version, Scala211Version)
lazy val `sttp-play26-json` = sttpPlayJson(Dependencies.play26Version, Scala212Version)
lazy val `sttp-play23-backend` = sttpPlayBackend(Dependencies.play23Version, Scala211Version)

lazy val `sttp-play` = project
  .in(file("."))
  .aggregate(
    `sttp-play23-json`,
    `sttp-play24-json`,
    `sttp-play25-json`,
    `sttp-play26-json`,
    `sttp-play23-backend`
  )