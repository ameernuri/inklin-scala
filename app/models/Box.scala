package models

import org.anormcypher._
import org.anormcypher.CypherParser._
import tools.Loggers._
import org.anormcypher.~

case class Box(id: Long, owner: Long, name: String, secret: Boolean)

object Box {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Box", log, params)

	def simpleReturn(owner: String = "owner", box: String = "box"): String = {
		s"""
		  |id($box) as boxId, id($owner) as boxOwnerId, $box.name, $box.secret
		""".stripMargin
	}

	val simple = {
		get[Long]("boxId") ~
		get[Long]("boxOwnerId") ~
		get[String]("box.name") ~
		get[Boolean]("box.secret") map {
			case id~owner~name~secret => Box(id, owner, name, secret)
		}
	}

	val withInkler = Box.simple ~ Inkler.simple map {
		case box ~ inkler => (box, inkler)
	}

	def create(owner: Long, name: String, secret: Boolean) = {
		log("create", Map("owner" -> owner, "name" -> name, "secret" -> secret))

		Cypher(
			"""
			  |START owner = node({owner})
			  |CREATE (box:Box {
			  | name: {name},
			  | secret: {secret},
			  | created: timestamp()
			  |})<-[:owns_box]-(owner),
			  |(box)-[:is_dependent_on]->(owner)
			""".stripMargin
		).on(
			"owner" -> owner,
			"name" -> name,
			"secret" -> secret
		).execute()
	}

	def view(id: Long): Seq[Inkle] = {
		log("view", Map("id" -> id))

		Cypher(
			s"""
			  |START box = node({id})
			  |MATCH (inkle:Inkle)-[:added_into]->(box),
			  |(inkler:Inkler)-[:owns_inkle]->(inkle),
			  |(inkle)-[:has_parent]-(parent)
			  |RETURN ${Inkle.simpleReturn()}
			""".stripMargin
		).on(
			"id" -> id
		).as(Inkle.simple *)
	}

	def addInkler(boxId: Long, inklerId: Long) = {
		log("addInkler", Map("boxId" -> boxId))

		Cypher(
			"""
			  |START box = node({boxId}), inkler = node({inklerId})
			  |CREATE (inkler)-[:is_member {created: timestamp()}]->(box)
			""".stripMargin
		).on(
			"boxId"  -> boxId,
			"inklerId" -> inklerId
		).execute()
	}

	def removeInkler(boxId: Long, inklerId: Long) = {
		log("removeInkler", Map("boxId" -> boxId, "inklerId" -> inklerId))

		Cypher(
			"""
			  |START box = node({boxId}), inkler = node({inklerId})
			  |MATCH (inkler)-[membership:is_member]->(box)
			  |DELETE membership
			""".stripMargin
		).on(
			"boxId"  -> boxId,
			"inklerId" -> inklerId
		).execute()
	}

	def members(boxId: Long): Seq[Inkler] = {
		log("members", Map("boxId" -> boxId))

		Cypher(
			s"""
			  |START box = node({boxId})
			  |MATCH (inkler:Inkler)-[:is_member]->(box)
			  |RETURN ${Inkler.simpleReturn()}
			""".stripMargin
		).on(
			"boxId"  -> boxId
		).as(Inkler.simple *)
	}

	def find(boxId: Long): Option[Box] = {
		log("find", Map("boxId" -> boxId))

		Cypher(
			s"""
			  |START box = node({boxId})
			  |MATCH (inkler:Inkler)-[:owns_box]->(box)
			  |RETURN ${simpleReturn(owner = "inkler")}
			""".stripMargin
		).on(
			"boxId"  -> boxId
		).as(simple.singleOpt)
	}

	def findWithInkler(boxId: Long): Option[(Box, Inkler)] = {
		log("findWithInkler", Map("boxId" -> boxId))

		Cypher(
			s"""
			  |START box = node({boxId})
			  |MATCH (inkler)-[:owns_box]->(box)
			  |RETURN ${simpleReturn(owner = "inkler")}, ${Inkler.simpleReturn()}
			""".stripMargin
		).on(
			"boxId"  -> boxId
		).as(withInkler.singleOpt)
	}

	def findOwned(inklerId: Long): Seq[Box] = {
		log("findOwned", Map("inklerId" -> inklerId))

		Cypher(
			s"""
			  |START inkler = node({inklerId})
			  |MATCH (inkler)-[:owns_box]->(box)
			  |WHERE box.secret = false
			  |RETURN ${simpleReturn("inkler")}
			""".stripMargin
		).on(
			"inklerId"   -> inklerId
		).as(simple *)
	}

	def findOwnedWithSecret(inklerId: Long): Seq[Box] = {
		log("findOwnedWithSecret", Map("inklerId" -> inklerId))

		Cypher(
			s"""
			  |START inkler = node({inklerId})
			  |MATCH (inkler)-[:owns_box]->(box)
			  |RETURN ${simpleReturn("inkler")}
			""".stripMargin
		).on(
			"inklerId"   -> inklerId
		).as(simple *)
	}

	def findSecret(inklerId: Long): Seq[Box] = {
		log("findSecret", Map("inklerId" -> inklerId))

		Cypher(
			s"""
			  |START inkler = node({inklerId})
			  |MATCH (inkler)-[:owns_box]->(box)
			  |WHERE box.secret = true
			  |RETURN ${simpleReturn("inkler")}
			""".stripMargin
		).on(
			"inklerId" -> inklerId
		).as(simple *)
	}

	def findNonInvitedByOwner(ownerId: Long, inklerId: Long): Seq[Box] = {
		log("findNonInvitedByOwner", Map("ownerId" -> ownerId, "inklerId" -> inklerId))

		Cypher(
			s"""
			  |START owner = node({ownerId}), inkler = node({inklerId})
			  |MATCH (owner)-[:owns_box]->(box)
			  |WHERE not(
			  | (inkler)-[:is_member]->(box)
			  |)
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"inklerId" -> inklerId,
			"ownerId" -> ownerId
		).as(simple *)
	}

	def findInvitedByOwner(ownerId: Long, inklerId: Long): Seq[Box] = {
		log("findInvitedByOwner", Map("ownerId" -> ownerId, "inklerId" -> "inklerId"))

		Cypher(
			s"""
			  |START owner = node({ownerId}), inkler = node({inklerId})
			  |MATCH (owner)-[:owns_box]->(box)
			  |WHERE (inkler)-[:is_member]->(box)
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"inklerId"     -> inklerId,
			"ownerId"  -> ownerId
		).as(simple *)
	}

	def findInvited(inklerId: Long): Seq[Box] = {
		log("findInvited", Map("inklerId" -> inklerId))

		Cypher(
			s"""
			  |START inkler = node({inklerId})
			  |MATCH (owner)-[:owns_box]->(box)
			  |WHERE (inkler)-[:is_member]->(box)
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"inklerId" -> inklerId
		).as(simple *)
	}

  def findFollowed(inklerId: Long): Seq[Box] = {
	  log("findFollowed", Map("inklerId" -> inklerId))

    Cypher(
      s"""
        |START inkler = node({inklerId})
        |MATCH (owner)-[:owns_box]->(box)
        |WHERE (inkler)-[:has_followed]->(box)
        |RETURN ${simpleReturn()}
      """.stripMargin
    ).on(
      "inklerId" -> inklerId
    ).as(simple *)
  }

	def suggestPopular(inkler: Long, count: Long): Seq[(Box, Inkler)] = {
		log("suggestPopular", Map("inkler" -> inkler, "count" -> count))

		Cypher(
			s"""
			  |START inkler = node({inklerId})
			  |MATCH (owner:Inkler)-[:owns_box]->(box:Box),
			  |(followers:Inkler)-[follows:has_followed]->(box)
			  |WHERE not(
			  | (inkler)-[:has_followed]->(box) and
			  | (inkler)-[:owns_box]->(box)
			  |)
			  |RETURN ${simpleReturn()}, ${Inkler.simpleReturn(inkler = "owner")},
			  |count(follows) as popularity
			  |ORDER BY popularity desc
			  |LIMIT {count}
			""".stripMargin
		).on(
			"count" -> count,
			"inkler" -> inkler
		).as(withInkler *)
	}

	def isOwner(boxId: Long, inklerId: Long): Boolean = {
		log("isOwner", Map("boxId" -> boxId, "inklerId" -> inklerId))

		Cypher(
			"""
			  |START box = node({boxId}), inkler = node({inklerId})
			  |MATCH (inkler)-[own:owns_box]->(box)
			  |RETURN count(own)
			""".stripMargin
		).on(
			"boxId" -> boxId,
			"inklerId" -> inklerId
		).as(scalar[Int].single) != 0
	}

	def isMember(box: Long, inklerId: Long): Boolean = {
		log("isMember", Map("box" -> box, "inklerId" -> inklerId))

		Cypher(
			"""
			  |START box = node({boxId}), inkler = node({inklerId})
			  |MATCH (inkler)-[membership:is_member]->(box)
			  |RETURN count(membership)
			""".stripMargin
		).on(
			"boxId" -> box,
			"inklerId" -> inklerId
		).as(scalar[Int].single) != 0
	}

	def isSecret(box: Long): Boolean = {
		log("isSecret", Map("box" -> box))

		Cypher(
			"""
			  |START box = node({boxId})
			  |RETURN box.secret
			""".stripMargin
		).on(
			"boxId" -> box
		).as(scalar[Boolean].single)
	}

	def follow(boxId: Long, inklerId: Long) = {
		log("follow", Map("boxId" -> boxId, "inklerId" -> inklerId))

		Cypher(
			"""
			  |START box = node({boxId}), inkler = node({inklerId})
			  |CREATE (inkler)-[:has_followed {created: timestamp()}]->(box)
			""".stripMargin
		).on(
			"boxId" -> boxId,
			"inklerId" -> inklerId
		).execute()
	}

	def unfollow(boxId: Long, inklerId: Long) = {
		Cypher(
			"""
			  |START box = node({boxId}), inkler = node({inklerId})
			  |MATCH (inkler)-[follow:has_followed]->(box)
			  |DELETE follow
			""".stripMargin
		).on(
			"boxId" -> boxId,
			"inklerId" -> inklerId
		).execute()
	}

	def viewFollowed(inklerId: Long): Seq[Box] = {
		Cypher(
			s"""
			  |START inkler = node({inklerId})
			  |MATCH (owner)-[:owns_box]->(box)
			  |WHERE (inkler)-[:has_followed]->(box)
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"inklerId" -> inklerId
		).as(simple *)
	}

	def hasFollowed(boxId: Long, inklerId: Long): Boolean = {
		Cypher(
			"""
			  |START box = node({boxId}), inkler = node({inklerId})
			  |MATCH (inkler)-[follow:has_followed]->(box)
			  |RETURN count(follow)
			""".stripMargin
		).on(
			"boxId" -> boxId,
			"inklerId" -> inklerId
		).as(scalar[Int].single) != 0
	}

	def followerCount(box: Long): Long = {
		Cypher(
			"""
			  |START box = node({boxId})
			  |MATCH (follower)-[:has_followed]->(box)
			  |RETURN count(follower)
			""".stripMargin
		).on(
			"box" -> box
		).as(scalar[Long].single)
	}
}
