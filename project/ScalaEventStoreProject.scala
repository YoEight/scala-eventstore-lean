import sbt._
import Keys._

import sbtprotobuf.{ ProtobufPlugin => PB }

object ScalaEventStoreProject extends Build {

  lazy val root = Project("scala-eventstore-lean", file("."))
    .settings(PB.protobufSettings : _*)
    .settings(coreSettings : _*)

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization := "io.coppermine",
    version := "0.1",
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.4", "2.11.7"),
    scalacOptions := Seq("-deprecation", "-feature"),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

  lazy val coreSettings = commonSettings ++ Seq(
    name := "scala-eventstore-lean",
    libraryDependencies :=
      Seq(
        "org.json4s" %% "json4s-native" % "3.3.0",
        "com.google.protobuf" % "protobuf-java" % "2.5.0",
        "org.specs2" %% "specs2-core" % "3.7" % "test"
      )
  )

  object Resolvers {

  }
}
