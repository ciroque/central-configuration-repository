import com.typesafe.sbt.SbtNativePackager._

organization  := "org.ciroque"

version       := "0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayVersion,
    "io.spray"            %%  "spray-routing" % sprayVersion,
    "io.spray"            %%  "spray-testkit" % sprayVersion  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaVersion,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaVersion   % "test",
    "org.specs2"          %%  "specs2-core"   % "2.3.11"      % "test",
    "io.spray"            %% "spray-json"     % "1.3.0",
    "org.mongodb"         %% "casbah"         % "2.8.1",
    "org.scalatest"       %% "scalatest"         % "2.2.4",
    "org.easymock"        % "easymock"       % "3.2"
  )
}

Revolver.settings

enablePlugins(JavaServerAppPackaging)

packageSummary := "Central Configuration Repository"

packageDescription := "Central Configuration Repository"

maintainer := "Steve Wagner <scalawagz@outlook.com>"

name := "ccr"
