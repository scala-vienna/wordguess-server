name := "wordguess-server"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "org.scalatest" %% "scalatest" % "2.0" % "test->default",
  "com.typesafe.akka" %% "akka-cluster"    % "2.2.3",
  "com.typesafe.akka" %% "akka-slf4j"      % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit"    % "2.2.3"
)

play.Project.playScalaSettings
