package models

import java.util.UUID._

import org.anormcypher._
import org.anormcypher.CypherParser._
import monkeys.Loggers._

case class User(uuid: String, username: String, name: String, email: String)

object User {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("User", log, params)

	val simple = {
		get[String]("user.uuid") ~
		get[String]("user.username") ~
		get[String]("user.name") ~
		get[String]("user.email")  map {
			case uuid~username~name~email =>
				User(uuid, username, name, email)
		}
	}

	def simpleReturn(user: String = "user"): String = {
		s"""
		  |$user.uuid, $user.username,
		  |$user.name, $user.email
		""".stripMargin
	}

	val withUser = Inkle.simple ~ User.simple map {
		case inkle ~ user => (inkle, user)
	}

	def authenticate(username: String, password: String): Option[User] = {
		log("authenticate", Map("username" -> username, "password" -> password))
		
		Cypher(
			s"""
			  |MATCH (user:User)
			  |WHERE (user.username = {username} OR user.email = {username}) AND
			  |user.password = {password}
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"username" -> username,
			"password" -> password
		).as(User.simple.singleOpt)
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
			  |CREATE (user:User {
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

	def find(uuid: String): Option[User] = {
		log("find", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (user:User {uuid: {uuid}})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(simple.singleOpt)
	}

	def findByUsername(username: String): Option[User] = {
		log("findByUsername", Map("username" -> username))

		Cypher(
			s"""
				|MATCH (user:User {username: {username}})
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
			  |MATCH (user:User)
			  |WHERE user.username = {username}
			  |RETURN user.uuid
			""".stripMargin
		).on(
			"username" -> username
		).as(scalar[String].singleOpt)
	}

	def findUsernameByUuid(uuid: String): Option[String] = {
		log("findUsernameByUuid", Map("uuid" -> uuid))

		Cypher(
			"""
			  |MATCH (user:User {uuid: {uuid}})
			  |RETURN user.username
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(scalar[String].singleOpt)
	}

	def findUsernameByUsernameOrEmail(usernameOrEmail: String): String = {
		log("findUsernameByUsernameOrEmail", Map("usernameOrEmail" -> usernameOrEmail))

		Cypher(
			"""
			  |MATCH (user:User)
			  |WHERE user.username = {usernameOrEmail} or
			  |user.email = {usernameOrEmail}
			  |RETURN user.username
			""".stripMargin
		).on(
			"usernameOrEmail" -> usernameOrEmail
		).as(scalar[String].single)
	}

	def usernameExists(username: String): Boolean = {
		log("usernameExists", Map("username" -> username))

		Cypher(
			"""
			  |MATCH (user:User)
			  |WHERE user.username = {username}
			  |RETURN count(user)
			""".stripMargin
		).on(
			"username" -> username
		).as(scalar[Int].single) != 0
	}

	def emailExists(email: String): Boolean = {
		log("emailExists", Map("email" -> email))

		Cypher(
			"""
			  |MATCH (user:User)
			  |WHERE user.email = {email}
			  |RETURN count(user)
			""".stripMargin
		).on(
			"email" -> email
		).as(scalar[Int].single) != 0
	}
}
