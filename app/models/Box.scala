package models

import org.anormcypher._
import org.anormcypher.CypherParser._

case class Box (id: Long, createdBy: Long, name: String, secret: Boolean, maxR: Long)

object Box {

	val simple = {
		get[Long]("box.id") ~
		get[Long]("box.created_by") ~
		get[String]("box.name") ~
		get[Boolean]("box.secret") ~
		get[Long]("box.max_r") map {
			case id~createdBy~name~secret~maxR => Box(id, createdBy, name, secret, maxR)
		}
	}

	val withInkler = Box.simple ~ Inkler.simple map {
		case box ~ inkler => (box, inkler)
	}

	def create(createdBy: Long, name: String, secret: Boolean, maxR: Long) = {

		Cypher(
			"""
			  insert into box values (
					{id}, {createdBy}, {name}, {secret}, {maxR}
				)
			"""
		).on(
			"createdBy"  -> createdBy,
			"name"       -> name,
			"secret"     -> secret,
			"maxR"       -> maxR
		).execute()
	}

	def view(id: Long): Seq[Inkle] = {
		Cypher(
			"""
			  select * from inkle
				where inkle.box_id = {id}
			"""
		).on(
			"id" -> id
		).as(Inkle.simple *)
	}

	def addInkler(boxId: Long, inklerId: Long) = {
		Cypher(
			"""
			  insert into box_member values(
					{boxId}, {inklerId}
			  )
			"""
		).on(
			"boxId"  -> boxId,
			"inklerId" -> inklerId
		).execute()
	}

	def removeInkler(boxId: Long, inklerId: Long) = {
		Cypher(
			"""
			  delete from box_member
			  where box_member.box_id = {boxId}
			  and box_member.inkler_id = {inklerId}
			"""
		).on(
			"boxId"  -> boxId,
			"inklerId" -> inklerId
		).execute()
	}

	def members(boxId: Long): Seq[Inkler] = {
		Cypher(
			"""
				select inkler.*
				from inkler join box_member
				on box_member.inkler_id = inkler.id
				where box_member.box_id = {boxId}
			"""
		).on(
			"boxId"  -> boxId
		).as(Inkler.simple *)
	}

	def findAll: Seq[Box] = {
		Cypher(
			"""
			  select * from box
			"""
		).as(simple *)
	}

	def find(boxId: Long): Option[Box] = {
		Cypher(
			"""
			  select * from box
			  where box.id = {boxId}
			"""
		).on(
			"boxId"  -> boxId
		).as(simple.singleOpt)
	}

	def findWithInkler(boxId: Long): Option[(Box, Inkler)] = {
		Cypher(
			"""
			  select box.*, inkler.*
			  from box join inkler
			  on box.created_by = inkler.id
			  where box.id = {boxId}
			"""
		).on(
			"boxId"  -> boxId
		).as(withInkler.singleOpt)
	}

	def findOwned(inklerId: Long): Seq[Box] = {
		Cypher(
			"""
			  select * from box
			  where
			  box.created_by = {inklerId}
				and
				box.secret = false
			"""
		).on(
			"inklerId"   -> inklerId
		).as(simple *)
	}

	def findOwnedWithSecret(inklerId: Long): Seq[Box] = {
		Cypher(
			"""
			  select * from box
			  where
			  box.created_by = {inklerId}
			"""
		).on(
			"inklerId"   -> inklerId
		).as(simple *)
	}

	def findSecret(inklerId: Long): Seq[Box] = {
		Cypher(
			"""
			  select * from box
			  where
			  box.created_by = {inklerId}
				and
				box.secret = true
			"""
		).on(
			"inklerId"   -> inklerId
		).as(simple *)
	}

	def findNotMemberedByCreator(creatorId: Long, inklerId: Long): Seq[Box] = {
		Cypher(
			"""
			  select * from box
			  where
			  box.created_by = {creatorId}
			  and box.secret = true
			  and box.id not in (
					select
					box_member.box_id
					from box_member
					where box_member.inkler_id = {inklerId}
			  )
			"""
		).on(
			"inklerId"     -> inklerId,
			"creatorId"  -> creatorId
		).as(simple *)
	}

	def findMemberedByCreator(creatorId: Long, inklerId: Long): Seq[Box] = {
		Cypher(
			"""
				select *
				from box join box_member
				on box.id = box_member.box_id
				where box.created_by = {creatorId}
				and box_member.inkler_id = {inklerId}
			"""
		).on(
			"inklerId"     -> inklerId,
			"creatorId"  -> creatorId
		).as(simple *)
	}

	def findMembered(inklerId: Long): Seq[Box] = {
		Cypher(
			"""
				select *
				from box join box_member
				on box.id = box_member.box_id
				and box_member.inkler_id = {inklerId}
			"""
		).on(
			"inklerId" -> inklerId
		).as(simple *)
	}

  def findFollowed(inklerId: Long): Seq[Box] = {
    Cypher(
      """
        select *
        from box join box_follow on box.id = box_follow.box_id and box_follow.follower_id = {inkler}
      """
    ).on(
      "inkler" -> inklerId
    ).as(simple *)
  }

	def suggestPopular(inkler: Long, count: Long): Seq[(Box, Inkler)] = {
		Cypher(
      """
				select box.*, inkler.*, count(box_follow.box_id) as follower_count
				from (
          box left join box_follow
          on box.id = box_follow.box_id
				)
				left join inkler
				on box.created_by = inkler.id

				where box.secret = false
				and box.id not in (
					select box.id from box left join box_follow
					on box.id = box_follow.box_id
					where box_follow.follower_id = {inkler}
				)
				and box.created_by <> {inkler}
				group by box.id, inkler.id
				order by follower_count desc limit {count}
      """
		).on(
			"count" -> count,
			"inkler" -> inkler
		).as(withInkler *)
	}

	def isOwner(box: Long, inklerId: Long): Boolean = {
		Cypher(
			"""
				select count(box.id) = 1
				from box
				where box.id = {box} and box.created_by = {inklerId}
			"""
		).on(
			"box" -> box,
			"inklerId" -> inklerId
		).as(scalar[Boolean].single)
	}

	def isMember(box: Long, inklerId: Long): Boolean = {
		Cypher(
			"""
			  select count(inkler.id) = 1
				from inkler join box_member
				on box_member.inkler_id = inkler.id
				where
				box_member.box_id = {box}
				and
				box_member.inkler_id = {inklerId}
			"""
		).on(
			"box" -> box,
			"inklerId" -> inklerId
		).as(scalar[Boolean].single)
	}

	def isSecret(box: Long): Boolean = {
		Cypher(
			"""
			  select count(box.id) = 1
			  from box
			  where
			  box.id = {box}
			  and box.secret = true
			"""
		).on(
			"box" -> box
		).as(scalar[Boolean].single)
	}

	def follow(boxId: Long, inklerId: Long) = {
		Cypher(
			"""
			  insert into box_follow values (
					{boxId}, {inklerId}
				)
			"""
		).on(
			"boxId" -> boxId,
			"inklerId" -> inklerId
		).execute()
	}

	def unfollow(boxId: Long, inklerId: Long) = {
		Cypher(
			"""
			  delete from box_follow
			  where box_follow.box_id = {boxId}
			  and box_follow.follower_id = {inklerId}
			"""
		).on(
			"boxId" -> boxId,
			"inklerId" -> inklerId
		).execute()
	}

	def viewFollowed(inklerId: Long): Seq[Box] = {
		Cypher(
			"""
			  select *
			  from box join box_follow
				on box_follow.box_id = box.id
			  where box_follow.follower_id = {inklerId}
			"""
		).on(
			"inklerId" -> inklerId
		).as(simple *)
	}

	def hasFollowed(boxId: Long, inklerId: Long): Boolean = {
		Cypher(
			"""
				select count(box_follow.box_id) = 1
				from box_follow
				where box_follow.box_id = {boxId}
				and box_follow.follower_id = {inklerId}
			"""
		).on(
			"boxId" -> boxId,
			"inklerId" -> inklerId
		).as(scalar[Boolean].single)
	}

	def followerCount(box: Long): Long = {
		Cypher(
			"""
				select count(*)
				from box_follow
				where box_id = {box}
			"""
		).on(
			"box" -> box
		).as(scalar[Long].single)
	}
}
