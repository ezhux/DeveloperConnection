name := "DeveloperConnection"

version := "0.1"

scalaVersion := "2.13.3"

resolvers += Resolver.sonatypeRepo("releases")

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.0"
val twitter4sVersion = "7.0"
val catsVersion = "2.0.0"
val scalaTestVersion = "3.2.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-stream"          % AkkaVersion,
  "com.typesafe.akka"   %% "akka-actor-typed"     % AkkaVersion,
  "com.typesafe.akka"   %% "akka-testkit"         % AkkaVersion         % "test",
  "com.typesafe.akka"   %% "akka-http"            % AkkaHttpVersion,
  "com.typesafe.akka"   %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.typesafe.akka"   %% "akka-http-testkit"    % AkkaHttpVersion,
  "com.danielasfregola" %% "twitter4s"            % twitter4sVersion,
  "org.typelevel"       %% "cats-core"            % catsVersion,
  "org.scalatest"       %% "scalatest"            % scalaTestVersion    % "test"
)