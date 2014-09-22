import com.typesafe.sbt.less.Import.LessKeys
import play.PlayScala

name := """Inklin"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers ++= Seq(
	"anormcypher" at "http://repo.anormcypher.org/",
	"Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
	"org.anormcypher" %% "anormcypher" % "0.5.1",
	"com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
	"com.amazonaws" % "aws-java-sdk" % "1.3.11",
	"com.drewnoakes" % "metadata-extractor" % "2.6.2",
	"com.typesafe.akka" % "akka-actor_2.11" % "2.3.3",
	"com.typesafe.akka" %% "akka-actor" % "2.3.4",
	"com.typesafe.akka" %% "akka-contrib" % "2.3.4",
	"com.typesafe.play.extras" %% "play-geojson" % "1.1.0",
	"org.webjars" % "bootstrap" % "3.0.2",
	"org.webjars" % "knockout" % "2.3.0",
	"org.webjars" % "requirejs" % "2.1.11-1",
	"org.webjars" % "leaflet" % "0.7.2",
	"org.webjars" % "rjs" % "2.1.11-1-trireme" % "test",
	"org.webjars" % "squirejs" % "0.1.0" % "test",
	"org.jsoup" % "jsoup" % "1.7.3",
	cache
)

scalacOptions += "-feature"

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

pipelineStages := Seq(digest, gzip)