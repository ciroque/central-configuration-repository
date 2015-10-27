
val appUser = "ccrsvc"

organization := "org.ciroque"

version := "0.4"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.4.0"
  val sprayVersion = "1.3.3"
  Seq(
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" %% "spray-json" % "1.3.0",
    "org.mongodb" %% "casbah" % "2.8.2",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "io.spray" %% "spray-testkit" % sprayVersion % "test",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "org.specs2" %% "specs2-core" % "2.3.11" % "test",
    "org.easymock" % "easymock" % "3.2" % "test",
    "net.debasishg" %% "redisclient" % "3.0",
    "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime",
    "com.gettyimages" %% "spray-swagger" % "0.5.1"
  )
}

Revolver.settings

enablePlugins(DebianPlugin)
enablePlugins(SbtNativePackager)
enablePlugins(JavaServerAppPackaging)

packageSummary := "Central Configuration Repository"

packageDescription := "Central Configuration Repository"

maintainer := "Steve Wagner <scalawagz@outlook.com>"

name := "central-configuration-repository"

//debianPackageDependencies in Debian ++= Seq("java-runtime-headless (>= 1.7)")

mappings in Universal <++= sourceDirectory  map { src =>
  val resources = src / "main" / "resources"
  val logback = resources / "logback.xml"
  val application = resources / "application.conf"
  Seq(logback -> "conf/logback.xml", application -> "conf/application.conf")
}

linuxPackageMappings in Debian <+= (version) map { (version) =>
  val tmp = IO.createTemporaryDirectory
  val tmpLog = tmp / "ccr" / "central-configuration-repository.log"
  IO.write(tmpLog, "")
  packageMapping(tmpLog -> "/var/log/ccr/central-configuration-repository.log") withUser appUser withGroup appUser
}

linuxPackageMappings <<= linuxPackageMappings map { mappings =>
  mappings map { linuxPackage =>
    linuxPackage.copy(
      fileData = linuxPackage.fileData.copy(
        user = appUser,
        group = appUser
      )
    ) withUser appUser withGroup appUser
  }
}
