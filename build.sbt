name := """play-secureaction"""

version := "1.2.0"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7")

organization := "com.ejisan"

publishTo := Some(Resolver.file("ejisan", file(Path.userHome.absolutePath+"/Development/repo.ejisan"))(Patterns(true, Resolver.mavenStyleBasePattern)))

lazy val `play-secureaction` = (project in file("."))

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.4.3" % Provided,
  "com.typesafe.play" %% "play-specs2" % "2.4.3" % Test
)
