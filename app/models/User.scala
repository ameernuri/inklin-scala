package models

import java.util.UUID._

import org.anormcypher._
import org.anormcypher.CypherParser._
import monkeys.DoLog._

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
			  | created: timestamp(),
			  | tourStep: 0
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

	def findByEmail(email: String): Option[User] = {
		log("findByEmail", Map("email" -> email))

		Cypher(
			s"""
				|MATCH (user:User {email: {email}})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
		  "email" -> email
		).as(simple.singleOpt)
	}

	def findByUsernameOrEmail(usernameOrEmail: String): Option[User] = {
		log("findByUsername", Map("username" -> usernameOrEmail))

		Cypher(
			s"""
				|MATCH (user:User)
				|WHERE user.username = {usernameOrEmail} OR
				|user.email = {usernameOrEmail}
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
		  "usernameOrEmail" -> usernameOrEmail
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

	def usernameOrEmailExists(usernameOrEmail: String): Boolean = {
		log("usernameOrEmailExists", Map("usernameOrEmail" -> usernameOrEmail))

		Cypher(
			"""
			  |MATCH (user:User)
			  |WHERE user.username = {usernameOrEmail} or
			  |user.email = {usernameOrEmail}
			  |RETURN count(user)
			""".stripMargin
		).on(
			"usernameOrEmail" -> usernameOrEmail
		).as(scalar[Int].single) != 0
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

	def createResetPasswordCode(uuid: String, code: String): Boolean = {
		log("createResetPasswordCode", Map("uuid" -> uuid, "code" -> code))

		Cypher(
			"""
				|MATCH (user:User {uuid: {uuid}})
			  |CREATE (user)-[:password_reset_code]->(code:PasswordResetCode {
			  | code: {code},
			  | expired: false,
			  | created: timestamp()
			  |}),
			  |(code)-[:is_dependent_on]->(user)
			""".stripMargin
		).on(
		  "uuid" -> uuid,
		  "code" -> code
		).execute()
	}

	def validatePasswordResetCode(email: String, code: String): Boolean = {
		log("validatePasswordResetCode", Map("email" -> email, "code" -> code))

		Cypher(
			"""
			  |MATCH (user:User)-[resetCode:password_reset_code]->(reset:PasswordResetCode)
			  |WHERE user.email = {email}
			  |and reset.code = {code}
			  |and reset.expired = false
			  |RETURN count(user) as count
			""".stripMargin
		).on(
		  "email" -> email,
		  "code" -> code
		).as(scalar[Long].single) != 0
	}

	def updatePassword(user: String, password: String): Boolean = {
		log("updatePassword", Map("user" -> user, "password" -> password))

		Cypher(
			"""
			  |MATCH (user:User {uuid: {uuid}})
			  |SET user.password = {password}, user.passwordUpdated = timestamp()
				|WITH user
			  |OPTIONAL MATCH (user)-[code:password_reset_code]->(resetCode)-[r]-()
			  |DELETE code, r, resetCode
			""".stripMargin
		).on(
		  "uuid" -> user,
		  "password" -> password
		).execute()
	}

	def updateTourStep(user: String, step: Int): Boolean = {
		log("updateTourStep", Map("user" -> user, "step" -> step))

		Cypher(
			"""
			  |MATCH (user:User {uuid: {uuid}})
			  |SET user.tourStep = {step}
			""".stripMargin
		).on(
		  "uuid" -> user,
		  "step" -> step
		).execute()
	}

	def getTourStep(uuid: String): Int = {
		log("getTourStep", Map("uuid" -> uuid))

		Cypher(
			"""
			  |MATCH (user:User {uuid: {uuid}})
			  |RETURN user.tourStep
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(scalar[Int].single)
	}
}
