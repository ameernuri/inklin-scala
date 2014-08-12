package models

import org.anormcypher._
import org.anormcypher.CypherParser._
import tools.Loggers._

case class Inkler(id: Long, username: String, firstName: String, lastName: String, email: String)

object Inkler {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Inkler", log, params)

	val simple = {
		get[Long]("inklerId") ~
		get[String]("inkler.username") ~
		get[String]("inkler.firstName") ~
		get[String]("inkler.lastName") ~
		get[String]("inkler.email")  map {
			case id~username~firstName~lastName~email =>
				Inkler(id, username, firstName, lastName, email)
		}
	}

	def simpleReturn(inkler: String = "inkler"): String = {
		s"""
		  |id($inkler) as inklerId, $inkler.username,
		  |$inkler.firstName, $inkler.lastName, $inkler.email
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

	def create(username: String, firstName: String, lastName: String, email: String, password: String) = {
		log("create", Map(
			"username" -> username,
			"firstName" -> firstName,
			"lastName" -> lastName,
			"email" -> email,
			"password" -> password
		))

		Cypher(
			"""
			  |CREATE (inkler:Inkler {
			  | username: {username},
			  | firstName: {firstName},
			  | lastName: {lastName},
			  | email: {email},
			  | password: {password},
			  | created: timestamp()
			  |})
			""".stripMargin
		).on(
			"username" -> username,
			"firstName" -> firstName,
			"lastName" -> lastName,
			"email" -> email.toLowerCase,
			"password" -> password
		).execute()
	}

	def find(id: Long): Option[Inkler] = {
		log("find", Map("id" -> id))

		Cypher(
			s"""
			  |START inkler = node({id})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"id" -> id
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

	def findIdByUsername(username: String): Option[Long] = {
		log("findIdByUsername", Map("username" -> username))

		Cypher(
			s"""
			  |MATCH (inkler:Inkler)
			  |WHERE inkler.username = {username}
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"username" -> username
		).as(scalar[Long].singleOpt)
	}

	def findUsernameById(id: Long): Option[String] = {
		log("findUsernameById", Map("id" -> id))

		Cypher(
			"""
			  |START inkler = node({id})
			  |RETURN inkler.username
			""".stripMargin
		).on(
			"id" -> id
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
