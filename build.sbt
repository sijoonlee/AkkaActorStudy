ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.6.19"
libraryDependencies +=
  "com.typesafe" % "config" % "1.4.2"
libraryDependencies +=
  "com.typesafe.akka" %% "akka-testkit" % "2.6.19" % Test
libraryDependencies +=
  "org.scalactic" %% "scalactic" % "3.2.13"
libraryDependencies +=
  "org.scalatest" %% "scalatest" % "3.2.13" % "test"

lazy val root = (project in file("."))
  .settings(
    name := "AkkaActors"
  )
