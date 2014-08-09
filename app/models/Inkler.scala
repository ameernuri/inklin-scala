package models

import org.anormcypher._
import org.anormcypher.CypherParser._

case class Inkler(id: Long, username: String, firstName: String, lastName: String, email: String, password: String, invitationCode: String)

object Inkler {
	val simple = {
		get[Long]("inkler.id") ~
		get[String]("inkler.username") ~
		get[String]("inkler.first_name") ~
		get[String]("inkler.last_name") ~
		get[String]("inkler.email") ~
		get[String]("inkler.invitation_code") ~
		get[String]("inkler.password") map {
			case id~username~firstName~lastName~email~password~invitationCode =>
				Inkler(id, username, firstName, lastName, email, password, invitationCode)
		}
	}

	val withInkler = Inkle.simple ~ Inkler.simple map {
		case inkle~inkler => (inkle, inkler)
	}

	def authenticate(username: String, password: String): Option[Inkler] = {
		Cypher(
			"""
				select * from inkler where
				(
					username = {username} or email = {username}
				) and password = {password}
			"""
		).on(
			"username" -> username,
			"password" -> password
		).as(Inkler.simple.singleOpt)
	}

	def create(username: String, firstName: String, lastName: String, email: String, password: String, invitationCode: String) = {

		Cypher(
			"""
				insert into inkler values(
					{id}, {username}, {firstName}, {lastName}, {email}, {password}, {invitationCode}
				)
			"""
		).on(
			"username" -> username,
			"firstName" -> firstName,
			"lastName" -> lastName,
			"email" -> email.toLowerCase,
			"password" -> password,
			"invitationCode" -> invitationCode
		).execute()
	}

	def findAll: Seq[Inkler] = {
		Cypher(
			"""
				select * from inkler
			"""
		).as(simple *)
	}

	def find(id: Long): Option[Inkler] = {
		Cypher(
			"""
				select * from inkler
				where inkler.id = {id}
			"""
		).on(
			"id" -> id
		).as(simple.singleOpt)
	}

	def findByUsername(username: String): Option[Inkler] = {
		val id = findIdByUsername(username).get
		find(id)
	}

	def findIdByUsername(username: String): Option[Long] = {
		Cypher(
			"""
				select inkler.id from inkler
				where inkler.username = {username}
			"""
		).on(
			"username" -> username
		).as(scalar[Long].singleOpt)
	}

	def findUsernameById(id: Long): Option[String] = {
		Cypher(
			"""
				select inkler.username
				from inkler
				where inkler.id = {id}
			"""
		).on(
			"id" -> id
		).as(scalar[String].singleOpt)
	}

	def findUsernameByUsernameOrEmail(usernameOrEmail: String): String = {
		Cypher(
			"""
				select inkler.username
				from inkler
				where
				inkler.username = {usernameOrEmail} or inkler.email = {usernameOrEmail}
			"""
		).on(
			"usernameOrEmail" -> usernameOrEmail
		).as(scalar[String].single)
	}

	def usernameExists(username: String): Boolean = {
		Cypher(
			"""
				select count(inkler.id) = 1
				from inkler
				where inkler.username = {username}
			"""
		).on(
			"username" -> username
		).as(scalar[Boolean].single)
	}

	def emailExists(email: String): Boolean = {
		Cypher(
			"""
				select count(inkler.id) = 1
				from inkler
				where inkler.email = {email}
			"""
		).on(
			"email" -> email
		).as(scalar[Boolean].single)
	}
}
