name := "inklin"

version := "1.0-SNAPSHOT"

resolvers ++= Seq(
	"anormcypher" at "http://repo.anormcypher.org/",
	"Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
	"org.anormcypher" %% "anormcypher" % "0.4.4",
	"com.typesafe" %% "play-plugins-mailer" % "2.2.0",
  cache
)

play.Project.playScalaSettings
