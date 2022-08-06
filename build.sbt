ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.6.19"

lazy val root = (project in file("."))
  .settings(
    name := "AkkaActors"
  )
