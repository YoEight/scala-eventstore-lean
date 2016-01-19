import sbt._
import Keys._

import sbtprotobuf.{ ProtobufPlugin => PB }
// import scalabuff.ScalaBuffPlugin._

object ScalaEventStoreProject extends Build {

  lazy val root = Project("scala-eventstore-lean", file("."))
    .settings(PB.protobufSettings : _*)
    .settings(coreSettings : _*)
//     .configs(ScalaBuff)

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization := "io.coppermine",
    version := "0.1",
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.4", "2.11.7"),
    scalacOptions := Seq("-deprecation", "-feature"),
    sourceDirectories in PB.protobufConfig <+= (PB.externalIncludePath in PB.protobufConfig)
  )

  lazy val coreSettings = commonSettings ++ Seq(
    name := "scala-eventstore-lean",
    libraryDependencies :=
      Seq(
        "org.json4s" %% "json4s-native" % "3.3.0",
        "com.google.protobuf" % "protobuf-java" % "2.6.1"
//        "net.sandrogrzicic" %% "scalabuff-runtime" % "1.4.0"
      )
  )

  object Resolvers {

  }
}
