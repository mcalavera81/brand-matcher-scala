organization := "net.pi"
name := "hotel-branding"

version := "0.14"

scalaVersion := "2.11.8"

scalacOptions += "-feature"
scalacOptions += "-language:implicitConversions"
scalacOptions += "-language:reflectiveCalls"

fork in Test := true

scalacOptions ++= Seq("-Xmax-classfile-name","128")
enablePlugins(DockerPlugin)

val piNexus = "Sonatype Nexus Repository Manager" at "http://nexus.pibenchmark.com/nexus/content/repositories/nexus-repo/"

resolvers +=  piNexus
resolvers += "Typesafe Releases" at "https://dl.bintray.com/typesafe/maven-releases/"

libraryDependencies +="com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "1.2.2"


val log4jVersion = "2.1"

libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.11.11"
libraryDependencies += "com.github.mpkorstanje" % "simmetrics-core" % "4.1.0"
libraryDependencies += "com.github.fakemongo" % "fongo" % "2.0.6" % "test"
libraryDependencies += "org.mongodb" % "mongo-java-driver" % "3.2.0" % "test"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % log4jVersion
libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % log4jVersion
libraryDependencies += "com.github.tototoshi" % "scala-csv_2.11" % "1.3.1"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.4.0"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.0"



libraryDependencies ++= Seq(
  "net.pi" % "matching-utils" % "1.0.8-RELEASE",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "commons-io" % "commons-io" % "2.4" % "test")


fork in run := true

credentials += Credentials("Sonatype Nexus Repository Manager",
  "nexus.pibenchmark.com",
  "admin",
  "admin")
publishTo := Some(piNexus)

isSnapshot := true

//assemblyJarName in assembly := "hotel-branding.jar"

// If you need to specify main classes manually, use packSettings and packMain
packSettings

// [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
packMain := Map("batch" -> "net.pi.driver.BatchProcessor")

//enablePlugins(DockerPlugin)

