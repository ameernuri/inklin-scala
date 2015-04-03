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
			  |}),
				|(admin)-[:is_group_member]->(group)
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"name" -> name,
			"description" -> description,
			"admin" -> admin
		).as(simple.singleOpt)
	}

	def update(uuid: String, name: String, description: String): Option[Group] = {
		log("find", Map("uuid" -> uuid, "name" -> name, "description" -> description))

		Cypher(
			s"""
			  |MATCH (admin:User)-[:is_group_admin]->(group:Group {uuid: {uuid}})
				|SET group.name = {name},
				|group.description = {description},
				|group.updated = timestamp()
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"uuid" -> uuid,
			"name" -> name,
			"description" -> description
		).as(simple.singleOpt)
	}

	def join(user: String, uuid: String): Boolean = {
		log("user", Map("uuid" -> uuid, "uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (user:User {uuid: {user}}), (group:Group {uuid: {uuid}})
				|CREATE (user)-[:is_group_member {created: timestamp()}]->(group)
			""".stripMargin
		).on(
			"uuid" -> uuid,
			"user" -> user
		).execute()
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

	def findOwned(user: String, limit: Int = 10): Seq[Group] = {
		log("findOwned", Map("user" -> user, "limit" -> limit))

		Cypher(
			s"""
			  |MATCH (admin:User {uuid: {user}})-[:is_group_admin]->(group:Group)
			  |RETURN ${simpleReturn()}
				|LIMIT {limit}
			""".stripMargin
		).on(
			"user" -> user,
			"limit" -> limit
		).as(simple.*)
	}

	def findMembered(user: String, limit: Int = 10): Seq[Group] = {
		log("findMembered", Map("user" -> user, "limit" -> limit))

		Cypher(
			s"""
			  |MATCH (user:User {uuid: {user}})-[:is_group_member]->(group:Group),
			  |(admin:User)-[:is_group_admin]->(group)
				|WITH user, admin, group
				|OPTIONAL MATCH (member:User)-[members:is_group_member]->(group:Group)
			  |RETURN ${simpleReturn()}, count(members) as memberCount
        |ORDER BY memberCount DESC
				|LIMIT {limit}
			""".stripMargin
		).on(
			"user" -> user,
			"limit" -> limit
		).as(simple.*)
	}

	def findPopular(user: String, limit: Int = 10): Seq[Group] = {
		log("findPopular", Map("user" -> user, "limit" -> limit))

		Cypher(
			s"""
			  |MATCH (user:User {uuid: {user}}),
				|(admin:User)-[:is_group_admin]->(group)
				|WHERE not((user)-[:is_group_member]->(group)) and
				|not((user)-[:is_group_admin]->(group))
				|WITH user, admin, group
				|OPTIONAL MATCH (member:User)-[members:is_group_member]->(group:Group)
			  |RETURN ${simpleReturn()}, count(members) as memberCount
				|ORDER BY memberCount DESC
				|LIMIT {limit}
			""".stripMargin
		).on(
			"user" -> user,
			"limit" -> limit
		).as(simple.*)
	}

	def ownsAny(user: String): Boolean = {
		log("find", Map("user" -> user))

		Cypher(
			s"""
			  |MATCH (admin:User {uuid: {user}})-[:is_group_admin]->(group:Group)
			  |RETURN count(group)
			""".stripMargin
		).on(
			"user" -> user
		).as(scalar[Int].single) > 0
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
		).as(scalar[Int].single) > 0
	}

	def isMember(user: String, group: String): Boolean = {
		log("isMember", Map("user" -> user, "group" -> group))

		Cypher(
			"""
			  |MATCH (user:User {uuid: {user}})-[m:is_group_member]->(group:Group {uuid: {group}})
			  |RETURN count(m)
			""".stripMargin
		).on(
			"user" -> user,
			"group" -> group
		).as(scalar[Int].single) > 0
	}

	def isAdmin(user: String, group: String): Boolean = {
		log("isAdmin", Map("user" -> user, "group" -> group))

		Cypher(
			"""
			  |MATCH (user:User {uuid: {user}})-[m:is_group_admin]->(group:Group {uuid: {group}})
			  |RETURN count(DISTINCT m)
			""".stripMargin
		).on(
			"user" -> user,
			"group" -> group
		).as(scalar[Int].single) > 0
	}

	def memberCount(group: String): Int = {
		log("memberCount", Map("group" -> group))

		Cypher(
			"""
			  |MATCH (user:User)-[m:is_group_member | is_group_admin]->(group:Group {uuid: {group}})
			  |RETURN count(DISTINCT m)
			""".stripMargin
		).on(
			"group" -> group
		).as(scalar[Int].single)
	}

	def originCount(group: String): Int = {
		log("originCount", Map("group" -> group))

		Cypher(
			"""
			  |MATCH (origin:Inkle)-[o:added_into]->(group:Group {uuid: {group}})
			  |RETURN count(DISTINCT o)
			""".stripMargin
		).on(
			"group" -> group
		).as(scalar[Int].single)
	}

	def inkleCount(group: String): Int = {
		log("inkleCount", Map("group" -> group))

		Cypher(
			"""
			  |MATCH (origin:Inkle)-[:added_into]->(group:Group {uuid: {group}})
				|WITH origin, group
				|OPTIONAL MATCH
				|(origin)<-[:has_parent*..]-(inkle)
			  |RETURN count(DISTINCT inkle) + count(DISTINCT origin)
			""".stripMargin
		).on(
			"group" -> group
		).as(scalar[Int].single)
	}
}
