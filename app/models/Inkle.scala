package models


import org.anormcypher._
import org.anormcypher.CypherParser._
import java.util.Date
import java.util.UUID._
import monkeys.DoLog._
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
	created: Date,
	deleted: Boolean
)

object Inkle {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Inkle", log, params)
	
	val simple = {
		get[String]("inkleUuid") ~
		get[String]("inkleOwnerUuid") ~
		get[String]("inkle.inkle") ~
		get[Option[String]]("inkleParentUuid") ~
		get[Long]("inkle.created") ~
		get[Boolean]("inkle.deleted") map {
			case id ~ owner ~ inkle ~ parentUuid ~ created ~ deleted=>
				Inkle(id, owner, inkle, parentUuid, new Date(created), deleted)
		}
	}

	def simpleReturn(inkle: String = "inkle", user: String = "user", parent: String = "parent"): String = {
		s"""
		  |$inkle.uuid as inkleUuid, $user.uuid as inkleOwnerUuid, $inkle.inkle,
		  |$parent.uuid as inkleParentUuid, $inkle.created, $inkle.deleted
		""".stripMargin
	}

	val withConnected = Inkle.simple ~ User.simple map {
		case inkle ~ user => (inkle, user)
	}

	val inkleParser = Inkle.simple ~ Inkle.simple map {
		case inkle ~ child => (inkle, child)
	}

	def create(userUuid: String, inkle: String, parentUuid: Option[String] = None, originUuid: Option[String] = None): String = {
		log("create", Map(
			"userUuid" -> userUuid,
			"parentUuid" -> parentUuid,
			"inkle" -> inkle
		))

		val parentQuery = if(parentUuid.isDefined) {
			if (originUuid.isDefined) ", (inkle)-[:has_parent]->(parent), (inkle)-[:has_origin]->(origin)"
			else ", (inkle)-[:has_parent]->(parent), (inkle)-[:has_origin]->(parent)"
		} else ""

		val parentNode = if(parentUuid.isDefined) {
			if (originUuid.isDefined) ", (parent:Inkle {uuid: {parentUuid}}), (origin:Inkle {uuid: {originUuid}})"
			else ", (parent:Inkle {uuid: {parentUuid}})"
		} else ""

		val cypher =
			s"""
			  |MATCH (user:User {uuid: {userUuid}})
			  |$parentNode
			  |CREATE (inkle:Inkle {
				| uuid: "$randomUUID",
			  | inkle: {inkle},
			  | created: timestamp(),
				| deleted: false
			  |}),
			  |(user)-[:owns_inkle]->(inkle)
			  |$parentQuery,
			  |(inkle)-[:is_dependent_on]->(user)
			  |RETURN inkle.uuid
			""".stripMargin

		if (parentUuid.isDefined) {
			Cypher(cypher).on(
			  "userUuid" -> userUuid,
			  "inkle" -> inkle,
			  "parentUuid" -> parentUuid.get,
			  "originUuid" -> originUuid.getOrElse("")
			).as(scalar[String].single)

		} else {
			Cypher(cypher).on(
			  "userUuid" -> userUuid,
			  "inkle" -> inkle
			).as(scalar[String].single)
		}
	}

	def edit(inkleUuid: String, inkle: String): (Inkle, User) = {
		log("edit", Map("inkleUuid" -> inkleUuid, "inkle" -> inkle))

		Cypher(
			s"""
				|MATCH (user:User)-[:owns_inkle]->(inkle:Inkle {uuid: {inkleUuid}})
				|WITH user, inkle
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
				|SET inkle.inkle = {newInkle}, inkle.deleted = false
				|RETURN ${simpleReturn()}, ${User.simpleReturn()}
			""".stripMargin
		).on(
			"inkleUuid" -> inkleUuid,
			"newInkle" -> inkle
		).as(withConnected.single)
	}

	def delete(inkleUuid: String): Boolean = {
		log("delete", Map("inkleUuid" -> inkleUuid))

		Cypher(
			"""
				|MATCH (inkle:Inkle {uuid: {inkleUuid}})
				|SET inkle.deleted = true, inkle.inkle = ""
			""".stripMargin
		).on(
			"inkleUuid" -> inkleUuid
		).execute()
	}

	def deleteWithChildren(inkleUuid: String): Boolean = {
		log("deleteWithChildren", Map("inkleUuid" -> inkleUuid))

		Cypher(
			"""
				|MATCH (inkle:Inkle {uuid: {inkleUuid}})
				|OPTIONAL MATCH ()-[c]-(children)-[:has_parent*..]->(inkle)
				|DELETE c, children
				|WITH inkle
				|OPTIONAL MATCH ()-[d]-(dependent)-[:is_dependent_on]->(inkle)
				|DELETE d, dependent
				|WITH inkle
				|MATCH (inkle)-[s]-()
				|DELETE s, inkle
			""".stripMargin
		).on(
			"inkleUuid" -> inkleUuid
		).execute()
	}

	def fetchPage(user: String, page: Int = 0, pageSize: Int = 10): Page[(Inkle, User)] = {
		log("fetchPage", Map("user" -> user, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (user:User {uuid: {userUuid}})-[:owns_inkle]->(inkle:Inkle)
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (child)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, child
				|OPTIONAL MATCH (children)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${User.simpleReturn()}, (count(child) * 2 + (count(children))) as rating
				|ORDER BY rating DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset,
			"userUuid" -> user
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on("userUuid" -> user).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def fetchPageByOrigin(origin: String, page: Int = 0, pageSize: Int = 10): Page[(Inkle, User)] = {
		log("fetchPageByOrigin", Map("origin" -> origin, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (origin:Inkle {uuid: {originUuid}})<-[:has_origin]-(inkle:Inkle),
			  |(user:User)-[:owns_inkle]->(inkle:Inkle)
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (child)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, child
				|OPTIONAL MATCH (children)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${User.simpleReturn()}, (count(child) * 2 + (count(children))) as rating
				|ORDER BY rating DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset,
			"originUuid" -> origin
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on("originUuid" -> origin).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def originsPage(user: String, page: Int = 0, pageSize: Int = 10): Page[(Inkle, User)] = {
		log("originsPage", Map("user" -> user, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (user:User {uuid: {userUuid}})-[:owns_inkle]->(inkle:Inkle)
				|WHERE not((inkle)-[:has_parent]->())
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (child)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, child
				|OPTIONAL MATCH (children)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${User.simpleReturn()}, (count(child) * 2 + (count(children))) as rating
				|ORDER BY rating DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset,
			"userUuid" -> user
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on("userUuid" -> user).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def getPathLength(inkleUuid: String): Int = {
		log("getPathLength", Map("inkleUuid" -> inkleUuid))

		Cypher(
			s"""
			  |MATCH (child:Inkle { uuid: {inkle} })-[:has_parent*..]->(inkle:Inkle)
			  |RETURN count(inkle)
			""".stripMargin
		).on(
			"inkle" -> inkleUuid
		).as(scalar[Int].single)
	}

	def getParents(inkleUuid: String, page: Int = 0, pageSize: Int = 5): Page[(Inkle, User)] = {
		log("getParents", Map("inkleUuid" -> inkleUuid, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (child:Inkle { uuid: {inkle} })-[:has_parent*..]->(inkle:Inkle),
				|(user)-[:owns_inkle]->(inkle)
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${User.simpleReturn()}
				|ORDER BY inkle.created DESC
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

	def getOrigin(inkleUuid: String): Option[(Inkle, User)] = {
		log("getOrigin", Map("inkleUuid" -> inkleUuid))

		Cypher(
			s"""
			  |MATCH (descendant {uuid: {inkleUuid}})-[:has_origin]->(inkle),
				|(user:User)-[:owns_inkle]->(inkle)
				|WITH user, inkle
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent {uuid: {parentUuid}})
			  |RETURN DISTINCT ${simpleReturn()}, ${User.simpleReturn()}
			""".stripMargin
		).on(
			"inkleUuid" -> inkleUuid
		).as(withConnected.singleOpt)
	}

	def getOriginUuid(inkleUuid: String): Option[String] = {
		log("getOriginUuid", Map("inkleUuid" -> inkleUuid))

		Cypher(
			s"""
			  |MATCH (inkle {uuid: {inkleUuid}})-[:has_origin]->(origin)
			  |RETURN DISTINCT origin.uuid
			""".stripMargin
		).on(
			"inkleUuid" -> inkleUuid
		).as(scalar[String].singleOpt)
	}

	def findChildren(parentUuid: String): Seq[(Inkle, User)] = {
		log("findChildren", Map("parentUuid" -> parentUuid))

		Cypher(
			s"""
			  |MATCH (inkle)-[:has_parent]->(parent {uuid: {parentUuid}}),
			  |(user)-[:owns_inkle]->(inkle)
			  |RETURN DISTINCT ${simpleReturn()}, ${User.simpleReturn()}
			""".stripMargin
		).on(
			"parentUuid" -> parentUuid
		).as(withConnected.*)
	}

	def findPageOfChildren(parentUuid: String, page: Int = 0, pageSize: Int = 5): Page[(Inkle, User)] = {
		log("findChildren", Map("parentUuid" -> parentUuid))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (inkle)-[:has_parent]->(parent {uuid: {parentUuid}}),
			  |(user)-[:owns_inkle]->(inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (child)-[:has_parent]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${User.simpleReturn()}, count(child) as childCount
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

	def getParent(uuid: String): (Inkle, User) = {
		log("getParent", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (child {uuid: {childUuid}})-[:has_parent]->(inkle:Inkle),
			  |(inkle)-[:has_parent]->(parent),
			  |(user)-[:owns_inkle]->(inkle)
			  |RETURN DISTINCT ${simpleReturn()}, ${User.simpleReturn()},
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

	def find(uuid: String): (Inkle, User) = {
		log("find", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (user)-[:owns_inkle]->(inkle:Inkle {uuid: {inkleUuid}})
			  |WITH inkle, user
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |RETURN DISTINCT ${simpleReturn()}, ${User.simpleReturn()}
			""".stripMargin
		).on(
			"inkleUuid" -> uuid
		).as(withConnected.single)
	}

	def findFollowed(userUuid: String, page: Int = 0, pageSize: Int = 10): Page[(Inkle, User)] = {
		log("findFollowed", Map("userUuid" -> userUuid, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (follower:User {uuid: {followerUuid}}),
				|(user)-[:owns_inkle]->(inkle)
			  |WITH follower, inkle, user
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${User.simpleReturn()},
			  |ORDER BY inkle.created desc
			  |SKIP {offset}
			  |LIMIT {pageSize}
			""".stripMargin
		).on(
			"followerUuid" -> userUuid,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on(
			"followerUuid" -> userUuid
		).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def suggest(user: String, q: String): Page[(Inkle, User)] = {
		log("suggest", Map("user" -> user, "q" -> q))

		val page = 0
		val pageSize = 3

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (user:User {uuid: {userUuid}}),
				|(user)-[:owns_inkle]->(inkle)
				|WHERE
        |(
        |  inkle.inkle =~ {query} or
        |  ('.*' + inkle.inkle) =~ {query} or
        |  ('.*' + inkle.inkle + '.*') =~ {query}
        |)
			  |WITH inkle, user
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${simpleReturn()}, ${User.simpleReturn()}
			  |ORDER BY inkle.created desc
			  |SKIP {offset}
			  |LIMIT {pageSize}
			""".stripMargin
		).on(
			"userUuid" -> user,
			"query" -> q,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on(
			"userUuid" -> user,
			"query" -> q
		).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}
}
