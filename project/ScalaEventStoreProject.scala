import sbt._
import Keys._

object ScalaEventStoreProject extends Build {
  lazy val root = Project("scala-eventstore-lean", file(".")).settings(coreSettings : _*)

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization := "io.coppermine",
    version := "0.1",
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.4", "2.11.7"),
    scalacOptions := Seq("-deprecation", "-feature")
  )

  lazy val coreSettings = commonSettings ++ Seq(
    name := "scala-eventstore-lean",
    libraryDependencies :=
      Seq(
        "org.json4s" %% "json4s-native" % "3.3.0"
      )
  )

  object Resolvers {

  }
}
