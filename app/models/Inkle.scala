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

case class FullInkle(
	uuid: String,
	owner: String,
	inkle: String,
	kind: String,
	parentUuid: Option[String],
	created: Date,
	childrenCount: Int,
	descendantCount: Int,
	deleted: Boolean
)

object Inkle {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Inkle", log, params)

	private def prepareQuery(query: String):String = {


		val fixed = query.trim.replaceAll("\\s+", " ")

		val softened = fixed
			.replaceAll("a", "(a)*")
			.replaceAll("ee", "(e|i)*")
			.replaceAll("e", "(e)*")
			.replaceAll("i", "(i|y|e)*")
			.replaceAll("o", "(o|u)*")
			.replaceAll("u", "(u|o)*")
			.replaceAll("y", "(y|ie|i)*")


		val ands = "(?=.*" + softened.replaceAll(" ", ".*)(?=.*") + ".*)"
		val self = ".*" + softened + ".*"

		"(?i)" + ands +  ".*|" + self
	}
	
	val simple = {
		get[String]("inkleUuid") ~
		get[String]("inkleOwnerUuid") ~
		get[String]("inkle.inkle") ~
		get[Option[String]]("inkleParentUuid") ~
		get[Long]("inkle.created") ~
		get[Boolean]("inkle.deleted") map {
			case id ~ owner ~ inkle ~ parentUuid ~ created ~ deleted =>
				Inkle(id, owner, inkle, parentUuid, new Date(created), deleted)
		}
	}

	val full = {
		get[String]("inkleUuid") ~
		get[String]("inkleOwnerUuid") ~
		get[String]("inkle.inkle") ~
		get[String]("inkle.kind") ~
		get[Option[String]]("inkleParentUuid") ~
		get[Long]("inkle.created") ~
		get[Int]("childrenCount") ~
		get[Int]("descendantCount") ~
		get[Boolean]("inkle.deleted") map {
			case id ~ owner ~ inkle ~ kind ~ parentUuid ~ created ~ childrenCount ~ descendantCount ~ deleted =>
				FullInkle(id, owner, inkle, kind, parentUuid, new Date(created), childrenCount, descendantCount, deleted)
		}
	}

	def simpleReturn(inkle: String = "inkle", user: String = "user", parent: String = "parent"): String = {
		s"""
		  |$inkle.uuid as inkleUuid, $user.uuid as inkleOwnerUuid, $inkle.inkle,
		  |$parent.uuid as inkleParentUuid, $inkle.created, $inkle.deleted
		""".stripMargin
	}

	def fullReturn(inkle: String = "inkle", user: String = "user", parent: String = "parent"): String = {
		s"""
		  |$inkle.uuid as inkleUuid, $user.uuid as inkleOwnerUuid, $inkle.inkle,
		  |$parent.uuid as inkleParentUuid, $inkle.created, $inkle.deleted, $inkle.kind,
			|count(DISTINCT children) as childrenCount, count(DISTINCT descendant) as descendantCount
		""".stripMargin
	}

	val withUserParser = Inkle.simple ~ User.simple map {
		case inkle ~ user => (inkle, user)
	}

	val fullParser = Inkle.full ~ User.simple map {
		case inkle ~ user => (inkle, user)
	}

	def create(
		          userUuid: String, inkle: String, parentUuid: Option[String] = None,
		          originUuid: Option[String] = None, groupUuid: Option[String] = None
		          ): String = {
		log("create", Map(
			"userUuid" -> userUuid,
			"parentUuid" -> parentUuid,
			"groupUuid" -> groupUuid,
			"inkle" -> inkle
		))

		parentUuid.map { parent =>
			Cypher(
				s"""
			  |MATCH (user:User {uuid: {userUuid}}),
				|(parent:Inkle {uuid: {parentUuid}}),
				|(parent)-[:has_origin]->(origin:Inkle),
				|(parent)-[:has_depth]->(parentDepth:Depth)
				|MERGE (depth:Depth {depth: (parentDepth.depth + 1)})
			  |CREATE (inkle:Inkle {
				| uuid: "$randomUUID",
			  | inkle: {inkle},
			  | kind: "normal",
			  | created: timestamp(),
				| deleted: false
			  |}),
			  |(user)-[:owns_inkle]->(inkle),
				|(inkle)-[:has_parent]->(parent),
				|(inkle)-[:has_origin]->(origin),
				|(inkle)-[:has_depth]->(depth)
			  |RETURN inkle.uuid
			""".stripMargin
			).on(
		    "userUuid" -> userUuid,
			  "inkle" -> inkle,
			  "parentUuid" -> parent
			).as(scalar[String].single)
		}.getOrElse {
			Cypher(
				s"""
			  |MATCH (user:User {uuid: {userUuid}}),
				|(depth:Depth {depth: 0})
				|${groupUuid.map {group => s", (group:Group {uuid: '$group'})"}.getOrElse("")}
			  |CREATE (inkle:Inkle {
				| uuid: "$randomUUID",
			  | inkle: {inkle},
			  | kind: "normal",
			  | created: timestamp(),
				| deleted: false
			  |}),
			  |(user)-[:owns_inkle]->(inkle),
				|(inkle)-[:has_origin]->(inkle), // it's it's own origin
				|(inkle)-[:has_depth]->(depth)
				|${groupUuid.map {group => ", (inkle)-[:added_into]->(group)" }.getOrElse("")}
			  |RETURN inkle.uuid
			""".stripMargin
			).on(
		    "userUuid" -> userUuid,
			  "inkle" -> inkle
			).as(scalar[String].single)
		}
	}

	def link(from: String, to: String): Boolean = {
		log("link", Map("from" -> from, "to" -> to))

		// todo: what's wrong with this thing! passing parameters not working!
		Cypher(
			s"""
				|MATCH (from:Inkle {uuid: "$from"}), (to:Inkle {uuid: "$to"})
				|CREATE (from)-[:linked_to {created: timestamp()}]->(to)
			""".stripMargin
		).on(
			"form" -> from.trim,
			"to" -> to.trim
		).execute()
	}

	def linkShortcut(from: String, to: String, user: String): String = {
		log("link", Map("from" -> from, "to" -> to, "user" -> user))

		Cypher(
			s"""
			  |MATCH (user:User {uuid: {userUuid}}),
				|(parent:Inkle {uuid: {parentUuid}}),
				|(parent)-[:has_origin]->(origin:Inkle),
				|(parent)-[:has_depth]->(parentDepth:Depth),
				|(to:Inkle {uuid: {toUuid}})
				|MERGE (depth:Depth {depth: (parentDepth.depth + 1)})
			  |CREATE (inkle:Inkle:LinkInkle {
				| uuid: "$randomUUID",
			  | inkle: "",
			  | created: timestamp(),
				| deleted: false,
				| kind: "link"
			  |})-[:points_to]->(to),
			  |(user)-[:owns_inkle]->(inkle),
				|(inkle)-[:has_parent]->(parent),
				|(inkle)-[:has_origin]->(origin),
				|(inkle)-[:has_depth]->(depth)
				|RETURN inkle.uuid
			""".stripMargin
		).on(
	    "userUuid" -> user,
			"parentUuid" -> from,
			"toUuid" -> to
		).as(scalar[String].single)
	}

	def edit(inkleUuid: String, inkle: String): (FullInkle, User) = {
		log("edit", Map("inkleUuid" -> inkleUuid, "inkle" -> inkle))

		Cypher(
			s"""
				|MATCH (user:User)-[:owns_inkle]->(inkle:Inkle {uuid: {inkleUuid}})
				|WITH user, inkle
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
				|SET inkle.inkle = {newInkle}, inkle.deleted = false
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children:Inkle)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, children
				|OPTIONAL MATCH (descendant:Inkle)-[:has_parent*..]->(inkle)
				|RETURN ${fullReturn()}, ${User.simpleReturn()}
			""".stripMargin
		).on(
			"inkleUuid" -> inkleUuid,
			"newInkle" -> inkle
		).as(fullParser.single)
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
				|MATCH (inkle)-[s]-()
				|DELETE s, inkle
			""".stripMargin
		).on(
			"inkleUuid" -> inkleUuid
		).execute()
	}

	def fetchPage(user: String, page: Int = 0, pageSize: Int = 10): Page[(FullInkle, User)] = {
		log("fetchPage", Map("user" -> user, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (user:User {uuid: {userUuid}})-[:owns_inkle]->(inkle:Inkle)
				|WHERE NOT((inkle)-[:has_parent]->())
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children:Inkle)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, children
				|OPTIONAL MATCH (descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${fullReturn()}, ${User.simpleReturn()}, (count(children) * 2 + (count(descendant))) as rating
				|ORDER BY rating DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset,
			"userUuid" -> user
		).as(fullParser.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on("userUuid" -> user).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def fetchPageByGroup(group: String, page: Int = 0, pageSize: Int = 10): Page[(FullInkle, User)] = {
		log("fetchPage", Map("group" -> group, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (group:Group {uuid: {groupUuid}})<-[:added_into]-(inkle:Inkle),
				|(user:User)-[:owns_inkle]->(inkle)
				|WHERE NOT((inkle)-[:has_parent]->())
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children:Inkle)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, children
				|OPTIONAL MATCH (descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${fullReturn()}, ${User.simpleReturn()}, (count(children) * 2 + (count(descendant))) as rating
				|ORDER BY rating DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset,
			"groupUuid" -> group
		).as(fullParser.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on("groupUuid" -> group).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def fetchPageByOrigin(origin: String, page: Int = 0, pageSize: Int = 5): Page[(FullInkle, User)] = {
		log("fetchPageByOrigin", Map("origin" -> origin, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (origin:Inkle {uuid: {originUuid}})<-[:has_origin]-(inkle:Inkle),
			  |(user:User)-[:owns_inkle]->(inkle:Inkle)
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children:Inkle)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, children
				|OPTIONAL MATCH (descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${fullReturn()}, ${User.simpleReturn()}, (count(children) * 2 + (count(descendant))) as rating
				|ORDER BY rating DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset,
			"originUuid" -> origin
		).as(fullParser.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on("originUuid" -> origin).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def originsPage(user: String, page: Int = 0, pageSize: Int = 100): Page[(FullInkle, User)] = {
		log("originsPage", Map("user" -> user, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (user:User {uuid: {userUuid}})-[:owns_inkle]->(inkle:Inkle)
				|WHERE not((inkle)-[:has_parent]->())
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children:Inkle)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, children
				|OPTIONAL MATCH (descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${fullReturn()}, ${User.simpleReturn()}, (count(children) * 2 + (count(descendant))) as rating
				|ORDER BY rating DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset,
			"userUuid" -> user
		).as(fullParser.*)

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

	def getParents(inkleUuid: String, page: Int = 0, pageSize: Int = 5): Page[(FullInkle, User)] = {
		log("getParents", Map("inkleUuid" -> inkleUuid, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (child:Inkle { uuid: {inkle} })-[:has_parent*..]->(inkle:Inkle),
				|(user)-[:owns_inkle]->(inkle)
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|OPTIONAL MATCH (children)-[:has_parent]->(inkle),
				|(descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${fullReturn()}, ${User.simpleReturn()}
				|ORDER BY inkle.created DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"inkle" -> inkleUuid,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(fullParser.*)

		val total = Cypher(
			s"""
			  |$query count(DISTINCT inkle)
			""".stripMargin
		).on(
	    "inkle" -> inkleUuid
		).as(scalar[Long].single)

		Page(inkles, page, offset, total)
	}

	def getPath(inkleUuid: String, page: Int = 0, pageSize: Int = 5): Page[(FullInkle, User)] = {
		log("getPath", Map("inkleUuid" -> inkleUuid, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
				|MATCH (child:Inkle { uuid: {inkle} })-[:has_origin]->(origin:Origin),
			  |(origin)<-[:has_parent*..]-(inkle:Inkle),
				|(user)-[:owns_inkle]->(inkle)
				|WITH inkle, user
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|OPTIONAL MATCH (children)-[:has_parent]->(inkle),
				|(descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${fullReturn()}, ${User.simpleReturn()}
				|ORDER BY inkle.created DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"inkle" -> inkleUuid,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(fullParser.*)

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
		).as(withUserParser.singleOpt)
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

	def findChildren(parentUuid: String): Seq[(FullInkle, User)] = {
		log("findChildren", Map("parentUuid" -> parentUuid))

		Cypher(
			s"""
			  |MATCH (inkle:Inkle)-[:has_parent]->(parent:Inkle {uuid: {parentUuid}}),
			  |(user:Inkle)-[:owns_inkle]->(inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children:Inkle)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, children
				|OPTIONAL MATCH (descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN DISTINCT ${fullReturn()}, ${User.simpleReturn()}
			""".stripMargin
		).on(
			"parentUuid" -> parentUuid
		).as(fullParser.*)
	}

	def findPageOfChildren(parentUuid: String, page: Int = 0, pageSize: Int = 5): Page[(FullInkle, User)] = {
		log("findChildren", Map("parentUuid" -> parentUuid))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (inkle:Inkle)-[:has_parent]->(parent:Inkle {uuid: {parentUuid}}),
			  |(user:User)-[:owns_inkle]->(inkle)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children:Inkle)-[:has_parent]->(inkle)
				|WITH inkle, user, parent, children
				|OPTIONAL MATCH (descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${fullReturn()}, ${User.simpleReturn()}, count(children) as childCount
				|ORDER BY childCount DESC
				|SKIP {offset}
				|LIMIT {pageSize}
			""".stripMargin
		).on(
			"parentUuid" -> parentUuid,
			"offset" -> offset,
			"pageSize" -> pageSize
		).as(fullParser.*)

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
			  |MATCH (child:Inkle {uuid: {childUuid}})-[:has_parent]->(inkle:Inkle),
			  |(inkle)-[:has_parent]->(parent),
			  |(user)-[:owns_inkle]->(inkle)
			  |RETURN DISTINCT ${simpleReturn()}, ${User.simpleReturn()},
			""".stripMargin
		).on(
			"childUuid" -> uuid
		).as(withUserParser.single)
	}

	def getParentUuid(uuid: String): Option[String] = {
		log("getParentUuid", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (inkle:Inkle {uuid: {childUuid}})-[:has_parent]->(parent:Inkle)
			  |RETURN DISTINCT parent.uuid
			""".stripMargin
		).on(
			"childUuid" -> uuid
		).as(str("parent.uuid").singleOpt)
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

	def findLink(uuid: String): Option[(Inkle, User)] = {
		log("findLink", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (link:Inkle {uuid: {inkleUuid}})-[:points_to]->(inkle:Inkle),
				|(user)-[:owns_inkle]->(inkle:Inkle)
			  |WITH inkle, user
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
			  |RETURN DISTINCT ${simpleReturn()}, ${User.simpleReturn()}
			""".stripMargin
		).on(
			"inkleUuid" -> uuid
		).as(withUserParser.singleOpt)
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
		).as(withUserParser.single)
	}

	def findFull(uuid: String): (FullInkle, User) = {
		log("find", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (user)-[:owns_inkle]->(inkle:Inkle {uuid: {inkleUuid}})
			  |WITH inkle, user
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children)-[:has_parent]->(inkle),
				|(descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN DISTINCT ${fullReturn()}, ${User.simpleReturn()}
			""".stripMargin
		).on(
			"inkleUuid" -> uuid
		).as(fullParser.single)
	}

	def findFollowed(userUuid: String, page: Int = 0, pageSize: Int = 10): Page[(FullInkle, User)] = {
		log("findFollowed", Map("userUuid" -> userUuid, "page" -> page, "pageSize" -> pageSize))

		val offset = page * pageSize

		val query =
			s"""
			  |MATCH (follower:User {uuid: {followerUuid}}),
				|(user)-[:owns_inkle]->(inkle)
			  |WITH follower, inkle, user
			  |OPTIONAL MATCH (inkle)-[:has_parent]->(parent)
				|WITH inkle, user, parent
				|OPTIONAL MATCH (children)-[:has_parent]->(inkle),
				|(descendant:Inkle)-[:has_parent*..]->(inkle)
			  |RETURN
			""".stripMargin

		val inkles = Cypher(
			s"""
			  |$query DISTINCT ${fullReturn()}, ${User.simpleReturn()},
			  |ORDER BY inkle.created desc
			  |SKIP {offset}
			  |LIMIT {pageSize}
			""".stripMargin
		).on(
			"followerUuid" -> userUuid,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(fullParser.*)

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
		).as(withUserParser.*)

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

	def originCount(user: String): Int = {
		log("originCount", Map("user" -> user))

		Cypher(
			"""
				|MATCH (user:User {uuid: {uuid}})-[:owns_inkle]->(inkle:Inkle)
				|WHERE not((inkle)-[:has_parent]->())
				|RETURN count(inkle) as count
			""".stripMargin
		).on(
			"uuid" -> user
		).as(scalar[Int].single)
	}

	def inkleCount(user: String): Int = {
		log("inkleCount", Map("user" -> user))

		Cypher(
			"""
				|MATCH (user:User {uuid: {uuid}})-[:owns_inkle]->(inkle:Inkle)
				|RETURN count(inkle) as count
			""".stripMargin
		).on(
			"uuid" -> user
		).as(scalar[Int].single)
	}

	def isAddedInGroup(inkle: String): Boolean = {
		log("isAddedInGroup", Map("inkle" -> inkle))

		Cypher(
			"""
				|MATCH (inkle:Inkle {uuid: {inkle}})-[:added_into]->(group:Group)
				|RETURN count(group) as count
			""".stripMargin
		).on(
			"inkle" -> inkle
		).as(scalar[Int].single) > 0
	}

	def findGroup(inkle: String): Option[Group] = {
		log("isAddedInGroup", Map("inkle" -> inkle))

		Cypher(
			s"""
				|MATCH (inkle:Inkle {uuid: {inkle}})-[:added_into]->(group:Group),
				|(admin:User)-[:is_group_admin]->(group)
				|RETURN ${Group.simpleReturn()}
			""".stripMargin
		).on(
			"inkle" -> inkle
		).as(Group.simple.singleOpt)
	}

	def search(query: String, user: String): Seq[Inkle] = {
		log("search", Map("query" -> query, "user" -> user))

		val regex = prepareQuery(query)

		println(regex)

		val cypher = Cypher(
			s"""
				|MATCH (user:User {uuid: {user}})-[:owns_inkle]->(inkle:Inkle)
				|WHERE (
				| inkle.inkle =~ {query} or
        | (inkle.inkle + '.*') =~ {query} or
        | ('.*' + inkle.inkle) =~ {query} or
        | ('.*' + inkle.inkle + '.*') =~ {query}
				|)
				|WITH user, inkle
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|RETURN DISTINCT ${simpleReturn()}
				|LIMIT 8
			 """.stripMargin
		)

		println(cypher)

		cypher.on(
			"user" -> user,
			"query" -> regex
		).as(simple.*)
	}

	def searchLinkable(query: String, user: String, link: String): Seq[(Inkle, User)] = {
		log("searchLinkable", Map("query" -> query, "user" -> user, "link" -> link))

		val regex = prepareQuery(query)

		println(regex)

		val cypher = Cypher(
			s"""
				|MATCH (user:User {uuid: {user}})-[:owns_inkle]->(inkle:Inkle),
				|(link:Inkle {uuid: {link}})
				|WHERE (
				|  not(link.uuid = inkle.uuid) and
				|  not((link)<-[:has_parent]-(:LinkInkle)-[:points_to]->(inkle)) and
				|  not((link)-[:points_to]->(inkle)) and (
				|    inkle.inkle =~ {query} or
        |    (inkle.inkle + '.*') =~ {query} or
        |    ('.*' + inkle.inkle) =~ {query} or
        |    ('.*' + inkle.inkle + '.*') =~ {query}
				|  )
				|)
				|WITH user, inkle, link
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|RETURN DISTINCT ${simpleReturn()}, ${User.simpleReturn()}
				|LIMIT 8
			 """.stripMargin
		)

		println(cypher)

		cypher.on(
			"user" -> user,
			"link" -> link,
			"query" -> regex
		).as(withUserParser.*)
	}

	def findOrigins(user: String, link: String): Seq[(Inkle, User)] = {
		log("findLinkableOrigins", Map("user" -> user, "link" -> link))

		val cypher = Cypher(
			s"""
				|MATCH (user:User {uuid: {user}})-[:owns_inkle]->(inkle:Inkle)
				|WHERE not((inkle)-[:has_parent]->())
				|WITH user, inkle
				|OPTIONAL MATCH (inkle)-[:has_parent]->(parent:Inkle)
				|RETURN DISTINCT ${simpleReturn()}, ${User.simpleReturn()}
				|LIMIT 8
			 """.stripMargin
		)

		println(cypher)

		cypher.on(
			"user" -> user,
			"link" -> link
		).as(withUserParser.*)
	}
}
