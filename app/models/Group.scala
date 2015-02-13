package models

import java.util.UUID._

import org.anormcypher._
import org.anormcypher.CypherParser._
import monkeys.DoLog._

case class Group(uuid: String, name: String, description: String, admin: String)

object Group {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Group", log, params)

	val simple = {
		get[String]("group.uuid") ~
		get[String]("group.name") ~
		get[String]("group.description") ~
		get[String]("admin.uuid")  map {
			case uuid~name~description~admin =>
				Group(uuid, name, description, admin)
		}
	}

	def simpleReturn(group: String = "group"): String = {
		s"""
		  |$group.uuid, $group.name, $group.description, admin.uuid
		""".stripMargin
	}

	def create(name: String, description: String, admin: String): Option[Group] = {
		log("create", Map(
			"name" -> name,
			"description" -> description,
			"admin" -> admin
		))

		Cypher(
			s"""
				|MATCH (admin:User {uuid: {admin}})
			  |CREATE (admin)-[:is_group_admin]->(group:Group {
        | uuid: "$randomUUID",
			  | name: {name},
			  | description: {description},
			  | created: timestamp()
			  |})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"name" -> name,
			"description" -> description,
			"admin" -> admin
		).as(simple.singleOpt)
	}

	def find(uuid: String): Option[Group] = {
		log("find", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (admin:User)-[:is_group_admin]->(group:Group {uuid: {uuid}})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(simple.singleOpt)
	}

	def findOwned(user: String): Seq[Group] = {
		log("find", Map("user" -> user))

		Cypher(
			s"""
			  |MATCH (admin:User {uuid: {user}})-[:is_group_admin]->(group:Group)
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"user" -> user
		).as(simple.*)
	}

	def exists(uuid: String): Boolean = {
		log("exists", Map("uuid" -> uuid))

		Cypher(
			"""
			  |MATCH (group:Group {uuid: {uuid}})
			  |RETURN count(group)
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(scalar[Int].single) != 0
	}
}
