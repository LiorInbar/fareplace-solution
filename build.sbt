name := "flight-service"

version := "0.1.0"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.23.6",
  "org.http4s" %% "http4s-dsl" % "0.23.6",
  "org.http4s" %% "http4s-circe" % "0.23.6",
  "io.circe" %% "circe-core" % "0.14.1",
  "io.circe" %% "circe-generic" % "0.14.1",
  "io.circe" %% "circe-parser" % "0.14.1",
  "org.typelevel" %% "cats-effect" % "3.2.9"
)

mainClass in Compile := Some("gpt.FlightService")
