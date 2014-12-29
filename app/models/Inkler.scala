package models

import java.util.UUID._

import org.anormcypher._
import org.anormcypher.CypherParser._
import monkeys.Loggers._

case class Inkler(uuid: String, username: String, name: String, email: String)

object Inkler {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Inkler", log, params)

	val simple = {
		get[String]("inkler.uuid") ~
		get[String]("inkler.username") ~
		get[String]("inkler.name") ~
		get[String]("inkler.email")  map {
			case uuid~username~name~email =>
				Inkler(uuid, username, name, email)
		}
	}

	def simpleReturn(inkler: String = "inkler"): String = {
		s"""
		  |$inkler.uuid, $inkler.username,
		  |$inkler.name, $inkler.email
		""".stripMargin
	}

	val withInkler = Inkle.simple ~ Inkler.simple map {
		case inkle~inkler => (inkle, inkler)
	}

	def authenticate(username: String, password: String): Option[Inkler] = {
		log("authenticate", Map("username" -> username, "password" -> password))
		
		Cypher(
			s"""
			  |MATCH (inkler:Inkler)
			  |WHERE inkler.username = {username} and
			  |inkler.password = {password}
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"username" -> username,
			"password" -> password
		).as(Inkler.simple.singleOpt)
	}

	def create(username: String, name: String, email: String, password: String) = {
		log("create", Map(
			"username" -> username,
			"name" -> name,
			"email" -> email,
			"password" -> password
		))

		Cypher(
			s"""
			  |CREATE (inkler:Inkler {
        | uuid: "$randomUUID",
			  | username: {username},
			  | name: {name},
			  | email: {email},
			  | password: {password},
			  | created: timestamp()
			  |})
			""".stripMargin
		).on(
			"username" -> username,
			"name" -> name,
			"email" -> email.toLowerCase,
			"password" -> password
		).execute()
	}

	def find(uuid: String): Option[Inkler] = {
		log("find", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (inkler:Inkler {uuid: {uuid}})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(simple.singleOpt)
	}

	def findByUsername(username: String): Option[Inkler] = {
		log("findByUsername", Map("username" -> username))

		Cypher(
			s"""
				|MATCH (inkler:Inkler {username: {username}})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
		  "username" -> username
		).as(simple.singleOpt)
	}

	def findUuidByUsername(username: String): Option[String] = {
		log("findUuidByUsername", Map("username" -> username))

		Cypher(
			s"""
			  |MATCH (inkler:Inkler)
			  |WHERE inkler.username = {username}
			  |RETURN inkler.uuid
			""".stripMargin
		).on(
			"username" -> username
		).as(scalar[String].singleOpt)
	}

	def findUsernameByUuid(uuid: String): Option[String] = {
		log("findUsernameByUuid", Map("uuid" -> uuid))

		Cypher(
			"""
			  |MATCH (inkler:Inkler {uuid: {uuid}})
			  |RETURN inkler.username
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(scalar[String].singleOpt)
	}

	def findUsernameByUsernameOrEmail(usernameOrEmail: String): String = {
		log("findUsernameByUsernameOrEmail", Map("usernameOrEmail" -> usernameOrEmail))

		Cypher(
			"""
			  |MATCH (inkler:Inkler)
			  |WHERE inkler.username = {usernameOrEmail} or
			  |inkler.email = {usernameOrEmail}
			  |RETURN inkler.username
			""".stripMargin
		).on(
			"usernameOrEmail" -> usernameOrEmail
		).as(scalar[String].single)
	}

	def usernameExists(username: String): Boolean = {
		log("usernameExists", Map("username" -> username))

		Cypher(
			"""
			  |MATCH (inkler:Inkler)
			  |WHERE inkler.username = {username}
			  |RETURN count(inkler)
			""".stripMargin
		).on(
			"username" -> username
		).as(scalar[Int].single) != 0
	}

	def emailExists(email: String): Boolean = {
		log("emailExists", Map("email" -> email))

		Cypher(
			"""
			  |MATCH (inkler:Inkler)
			  |WHERE inkler.email = {email}
			  |RETURN count(inkler)
			""".stripMargin
		).on(
			"email" -> email
		).as(scalar[Int].single) != 0
	}
}
