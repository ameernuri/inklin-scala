package models

import org.anormcypher._
import org.anormcypher.CypherParser._
import java.util.Date
import tools.Loggers._
import org.anormcypher.~

case class Page[A](items: Seq[A], page: Int, offset: Long) {
	lazy val prev = Option(page - 1).filter(_ >= 0)
	lazy val next = Option(page - 1)
}

case class Inkle(
	id: Long,
	owner: Long,
	inkle: String,
	boxId: Long,
	parentId: Option[Long],
	created: Date
)

object Inkle {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Inkle", log, params)
	
	val simple = {
		get[Long]("inkleId") ~
		get[Long]("inkleOwnerId") ~
		get[String]("inkle.inkle") ~
		get[Long]("inkleBoxId") ~
		get[Option[Long]]("inkleParentId") ~
		get[Long]("inkle.created") map {
			case id ~ owner ~ inkle ~ boxId ~ parentId ~ created =>
				Inkle(id, owner, inkle, boxId, parentId, new Date(created))
		}
	}

	def simpleReturn(inkle: String = "inkle", inkler: String = "inkler", box: String = "box", parent: String = "parent"): String = {
		s"""
		  |id($inkle) as inkleId, id($inkler) as inkleOwnerId, $inkle.inkle,
		  |id($box) as inkleBoxId, id($parent) as inkleParentId, $inkle.created
		""".stripMargin
	}

	val withConnected = Inkle.simple ~ Inkler.simple ~ Box.simple map {
		case inkle ~ inkler ~ box => (inkle, inkler, box)
	}

	val inkleParser = Inkle.simple ~ Inkle.simple map {
		case inkle ~ child => (inkle, child)
	}

	def create(inklerId: Long, box: Long, parentId: Option[Long], inkle: String): Long = {
		log("create", Map(
			"inklerId" -> inklerId,
			"box" -> box,
			"parentId" -> parentId,
			"inkle" -> inkle
		))

		val parentQuery = if(parentId.isDefined) {
			"(inkle)-[:has_parent]->(parent), (inkle)-[:is_dependent_on]->(box),"
		} else { "" }

		val parentNode = if(parentId.isDefined) { ", parent = node({parentId})" } else { "" }

		val cypher =
			s"""
			  |START inkler = node({inklerId}), box = node({boxId})
			  |$parentNode
			  |CREATE (inkle:Inkle {
			  | inkle: {inkle},
			  | created: timestamp()
			  |})-[:added_into]->(box),
			  |(inkler)-[:owns_inkle]->(inkle),
			  |$parentQuery
			  |(inkle)-[:is_dependent_on]->(inkler),
			  |(inkle)-[:is_dependent_on]->(box)
			  |RETURN id(inkle)
			""".stripMargin

		if (parentId.isDefined) {
			Cypher(cypher).on(
			  "inklerId" -> inklerId,
			  "inkle" -> inkle,
			  "boxId" -> box,
			  "parentId" -> parentId.get
			).as(scalar[Long].single)

		} else {
			Cypher(cypher).on(
			  "inklerId" -> inklerId,
			  "inkle" -> inkle,
			  "boxId" -> box
			).as(scalar[Long].single)
		}
	}

	def fetchPage(page: Int = 0, pageSize: Int = 10): Page[(Inkle, Inkler, Box)] = {
		log("fetchPage", Map("page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val inkles = Cypher(
			s"""
			  |MATCH (inkle:Inkle)-[:added_into]->(box),
			  |(inkle)-[:has_parent]->(parent),
			  |(inkler)-[:owns_inkle]->(inkle),
			  |(boxOwner)-[:owns_box]->(box)
			  |WHERE box.secret = false
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()},
			  |${Box.simpleReturn(owner = "boxOwner")}
				|ORDER BY inkle.created desc
				|LIMIT {pageSize}
				|SKIP {offset}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected *)

		Page(inkles, page, offset)
	}

	def findChildren(parentId: Long): Seq[(Inkle, Inkler, Box)] = {
		log("findChildren", Map("parentId" -> parentId))

		Cypher(
			s"""
			  |START parent = node({parentId})
			  |MATCH (inkle)-[:has_parent]->(parent),
			  |(inkle)-[:added_into]->(box),
			  |(inkler)-[:owns_inkle]->(inkle),
			  |(boxOwner)-[:owns_box]->(box)
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()},
			  |${Box.simpleReturn(owner = "boxOwner")}
			""".stripMargin
		).on(
			"parentId" -> parentId
		).as(withConnected *)
	}

	def getParent(id: Long): (Inkle, Inkler, Box) = {
		log("getParent", Map("id" -> id))

		Cypher(
			s"""
			  |START child = node({childId})
			  |MATCH (child)-[:has_parent]->(inkle:Inkle),
			  |(inkle)-[:has_parent]->(parent),
			  |(inkle)-[:added_into]->(box),
			  |(inkler)-[:owns_inkle]->(inkle),
			  |(boxOwner)-[:owns_box]->(box)
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()},
			  |${Box.simpleReturn(owner = "boxOwner")}
			""".stripMargin
		).on(
			"childId" -> id
		).as(withConnected.single)
	}

	def childrenCount(id: Long): Long = {
		log("childrenCount", Map("id" -> id))

		Cypher(
			"""
			  |START parent = node({parentId})
			  |MATCH (inkle:Inkle)-[:has_parent]->(parent)
			  |RETURN count(DISTINCT inkle) as count
			""".stripMargin
		).on(
			"parentId" -> id
		).as(scalar[Long] single)
	}

	def find(id: Long): (Inkle, Inkler, Box) = {
		log("find", Map("id" -> id))

		Cypher(
			s"""
			  |START inkle = node({inkleId})
			  |MATCH (inkle)-[:added_into]->(box),
			  |(inkler)-[:owns_inkle]->(inkle),
			  |(boxOwner)-[:owns_box]->(box)
			  |WITH inkle, box, inkler, boxOwner
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()},
			  |${Box.simpleReturn(owner = "boxOwner")}
			""".stripMargin
		).on(
			"inkleId" -> id
		).as(withConnected.single)
	}

	def findBoxId(id: Long): Long = {
		log("findBoxId", Map("id" -> id))

		Cypher(
			"""
			  |START inkle = node({inkleId})
			  |MATCH (inkle)-[:added_into]->(box)
			  |RETURN id(box) as boxId
			""".stripMargin
		).on(
			"inkleId" -> id
		).as(scalar[Long] single)
	}

	def findFollowed(inklerId: Long, page: Int = 0, pageSize: Int = 10): Page[(Inkle, Inkler, Box)] = {
		log("findFollowed", Map("inklerId" -> inklerId, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val inkles = Cypher(
			s"""
				|START follower = node({followerId})
			  |MATCH (inkle:Inkle)-[:added_into]->(box),
			  |(inkler)-[:owns_inkle]->(inkle),
			  |(boxOwner)-[:owns_box]->(box)
			  |WITH follower, inkle, box, inkler, boxOwner
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |WHERE (
			  | (follower)-[:has_followed]->(box)
			  |)
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()},
			  |${Box.simpleReturn(owner = "boxOwner")}
			  |ORDER BY inkle.created desc
			  |SKIP {offset}
			  |LIMIT {pageSize}
			""".stripMargin
		).on(
			"followerId" -> inklerId,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected *)

		Page(inkles, page, offset)
	}

	def findByBox(boxId: Long, page: Int = 0, pageSize: Int = 10): Page[(Inkle, Inkler, Box)] = {
		log("findByBox", Map("boxId" -> boxId, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val inkles = Cypher(
			s"""
			  |START box = node({boxId})
			  |MATCH (inkle:Inkle)-[:added_into]->(box),
			  |(inkler)-[:owns_inkle]->(inkle),
			  |(boxOwner)-[:owns_box]->(box)
			  |WITH box, inkle, inkler, boxOwner
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |RETURN DISTINCT ${simpleReturn()}, ${Inkler.simpleReturn()},
			  |${Box.simpleReturn(owner = "boxOwner")}
			  |ORDER BY inkle.created desc
			  |SKIP {offset}
			  |LIMIT {pageSize}
			""".stripMargin
		).on(
			"boxId" -> boxId,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected *)

		Page(inkles, page, offset)
	}

	def isInBox(inkle: Long, box: Long): Boolean = {
		log("isInBox", Map("inkle" -> inkle, "box" -> box))

		Cypher(
			"""
			  |START inkle = node({inkle}), box = node({box})
			  |MATCH (inkle)-[inBox:added_into]->(box)
			  |RETURN count(inBox) as count
			""".stripMargin
		).on(
			"inkle" -> inkle,
			"box" -> box
		).as(scalar[Int].single) != 0
	}
}
