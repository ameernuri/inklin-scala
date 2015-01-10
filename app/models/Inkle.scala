package models


import org.anormcypher._
import org.anormcypher.CypherParser._
import java.util.Date
import java.util.UUID._
import monkeys.Loggers._
import org.anormcypher.~

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
	lazy val prev = Option(page - 1).filter(_ >= 0)
	lazy val next = Option(page - 1)
}

case class Inkle(
	uuid: String,
	owner: String,
	inkle: String,
	parentUuid: Option[String],
	created: Date
)

object Inkle {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Inkle", log, params)
	
	val simple = {
		get[String]("inkleUuid") ~
		get[String]("inkleOwnerUuid") ~
		get[String]("inkle.inkle") ~
		get[Option[String]]("inkleParentUuid") ~
		get[Long]("inkle.created") map {
			case id ~ owner ~ inkle ~ parentUuid ~ created =>
				Inkle(id, owner, inkle, parentUuid, new Date(created))
		}
	}

	def simpleReturn(inkle: String = "inkle", inkler: String = "inkler", parent: String = "parent"): String = {
		s"""
		  |$inkle.uuid as inkleUuid, $inkler.uuid as inkleOwnerUuid, $inkle.inkle,
		  |$parent.uuid as inkleParentUuid, $inkle.created
		""".stripMargin
	}

	val withConnected = Inkle.simple ~ Inkler.simple map {
		case inkle ~ inkler => (inkle, inkler)
	}

	val inkleParser = Inkle.simple ~ Inkle.simple map {
		case inkle ~ child => (inkle, child)
	}

	def create(inklerUuid: String, parentUuid: Option[String], inkle: String): String = {
		log("create", Map(
			"inklerUuid" -> inklerUuid,
			"parentUuid" -> parentUuid,
			"inkle" -> inkle
		))

		val parentQuery = if(parentUuid.isDefined) {
			", (inkle)-[:has_parent]->(parent)"
		} else { "" }

		val parentNode = if(parentUuid.isDefined) { ", (parent:Inkle {uuid: {parentUuid}})" } else { "" }

		val cypher =
			s"""
			  |MATCH (inkler:Inkler {uuid: {inklerUuid}})
			  |$parentNode
			  |CREATE (inkle:Inkle {
				| uuid: "$randomUUID",
			  | inkle: {inkle},
			  | created: timestamp()
			  |}),
			  |(inkler)-[:owns_inkle]->(inkle)
			  |$parentQuery,
			  |(inkle)-[:is_dependent_on]->(inkler)
			  |RETURN inkle.uuid
			""".stripMargin

		if (parentUuid.isDefined) {
			Cypher(cypher).on(
			  "inklerUuid" -> inklerUuid,
			  "inkle" -> inkle,
			  "parentUuid" -> parentUuid.get
			).as(scalar[String].single)

		} else {
			Cypher(cypher).on(
			  "inklerUuid" -> inklerUuid,
			  "inkle" -> inkle
			).as(scalar[String].single)
		}
	}

	def edit(inkleUuid: String, inkle: String): (Inkle, Inkler) = {
		log("edit", Map("inkleUuid" -> inkleUuid, "inkle" -> inkle))

		Cypher(
			s"""
				|MATCH (inkler:Inkler)-[:owns_inkle]->(inkle:Inkle {uuid: {inkleUuid}})
				|WITH inkler, inkle
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
				|SET inkle.inkle = {newInkle}
				|RETURN ${simpleReturn()}, ${Inkler.simpleReturn()}
			""".stripMargin
		).on(
			"inkleUuid" -> inkleUuid,
			"newInkle" -> inkle
		).as(withConnected.single)
	}

	def fetchPage(page: Int = 0, pageSize: Int = 10): Page[(Inkle, Inkler)] = {
		log("fetchPage", Map("page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (inkler:Inkler)-[:owns_inkle]->(inkle:Inkle)
				|WITH inkle, inkler
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|WITH inkle, inkler, parent
				|OPTIONAL MATCH (child)-[:has_parent]->(inkle)
				|WITH inkle, inkler, parent, child
				|OPTIONAL MATCH (children)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()}, (count(child) * 3 + (count(children))) as rating
				|ORDER BY rating DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def getParents(inkleUuid: String, page: Int = 0, pageSize: Int = 5): Page[(Inkle, Inkler)] = {
		log("getParents", Map("inkleUuid" -> inkleUuid, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (child:Inkle { uuid: {inkle} })-[:has_parent*..]->(inkle:Inkle),
				|(inkler)-[:owns_inkle]->(inkle)
				|WITH inkle, inkler
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()}
				|ORDER BY inkle.created ASC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"inkle" -> inkleUuid,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on(
	    "inkle" -> inkleUuid
		).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def findChildren(parentUuid: String): Seq[(Inkle, Inkler)] = {
		log("findChildren", Map("parentUuid" -> parentUuid))

		Cypher(
			s"""
			  |MATCH (inkle)-[:has_parent]->(parent {uuid: {parentUuid}}),
			  |(inkler)-[:owns_inkle]->(inkle)
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()}
			""".stripMargin
		).on(
			"parentUuid" -> parentUuid
		).as(withConnected.*)
	}

	def findPageOfChildren(parentUuid: String, page: Int = 0, pageSize: Int = 5): Page[(Inkle, Inkler)] = {
		log("findChildren", Map("parentUuid" -> parentUuid))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (inkle)-[:has_parent]->(parent {uuid: {parentUuid}}),
			  |(inkler)-[:owns_inkle]->(inkle)
				|WITH inkle, inkler, parent
				|OPTIONAL MATCH (child)-[:has_parent]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()}, count(child) as childCount
				|ORDER BY childCount DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"parentUuid" -> parentUuid,
			"offset" -> offset,
			"pageSize" -> pageSize
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on(
			"parentUuid" -> parentUuid
		).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def getParent(uuid: String): (Inkle, Inkler) = {
		log("getParent", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (child {uuid: {childUuid}})-[:has_parent]->(inkle:Inkle),
			  |(inkle)-[:has_parent]->(parent),
			  |(inkler)-[:owns_inkle]->(inkle)
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()},
			""".stripMargin
		).on(
			"childUuid" -> uuid
		).as(withConnected.single)
	}

	def childrenCount(uuid: String): Long = {
		log("childrenCount", Map("uuid" -> uuid))

		Cypher(
			"""
			  |MATCH (inkle:Inkle)-[:has_parent]->(parent {uuid: {parentUuid}})
			  |RETURN count(inkle) as count
			""".stripMargin
		).on(
			"parentUuid" -> uuid
		).as(scalar[Long].single)
	}

	def descendantCount(uuid: String): Long = {
		log("descendantCount", Map("uuid" -> uuid))

		Cypher(
			"""
			  |MATCH (inkle:Inkle)-[:has_parent*..]->(parent {uuid: {parentUuid}})
			  |RETURN count(DISTINCT inkle) as count
			""".stripMargin
		).on(
			"parentUuid" -> uuid
		).as(scalar[Long].single)
	}

	def find(uuid: String): (Inkle, Inkler) = {
		log("find", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (inkler)-[:owns_inkle]->(inkle:Inkle {uuid: {inkleUuid}})
			  |WITH inkle, inkler
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()}
			""".stripMargin
		).on(
			"inkleUuid" -> uuid
		).as(withConnected.single)
	}

	def findFollowed(inklerUuid: String, page: Int = 0, pageSize: Int = 10): Page[(Inkle, Inkler)] = {
		log("findFollowed", Map("inklerUuid" -> inklerUuid, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (follower:Inkler {uuid: {followerUuid}}),
				|(inkler)-[:owns_inkle]->(inkle)
			  |WITH follower, inkle, inkler
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()},
			  |ORDER BY inkle.created desc
			  |SKIP {offset}
			  |LIMIT {pageSize}
			""".stripMargin
		).on(
			"followerUuid" -> inklerUuid,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on(
			"followerUuid" -> inklerUuid
		).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}
}
