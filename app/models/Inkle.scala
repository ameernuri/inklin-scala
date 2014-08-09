package models

import org.anormcypher._
import org.anormcypher.CypherParser._
import java.util.Date

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
	lazy val prev = Option(page - 1).filter(_ >= 0)
	lazy val next = Option(page - 1).filter(_ => (offset + items.size) < total)
}

case class Inkle(
	id: Long,
	createdBy: Long,
	inkle: String,
	boxId: Long,
	parentId: Option[Long],
	l: Long,
	r: Long,
	created: Date
)

object Inkle {
	
	val simple = {
		get[Long]("inkle.id") ~
		get[Long]("inkle.created_by") ~
		get[String]("inkle.inkle") ~
		get[Long]("inkle.box_id") ~
		get[Option[Long]]("inkle.parent_id") ~
		get[Long]("inkle.l") ~
		get[Long]("inkle.r") ~
		get[Long]("inkle.created") map {
			case id ~ createdBy ~ inkle ~ boxId ~ parentId ~ l ~ r ~ created =>
				Inkle(id, createdBy, inkle, boxId, parentId, l, r, new Date (created))
		}
	}

	val withConnected = Inkle.simple ~ Inkler.simple ~ Box.simple map {
		case inkle ~ inkler ~ box => (inkle, inkler, box)
	}

	val inkleParser = Inkle.simple ~ Inkle.simple map {
		case inkle ~ child => (inkle, child)
	}

	def create(inklerId: Long, parentBox: Option[Long], parentId: Option[Long], inkle: String) = {

		val boxId: Long = if (!parentBox.isDefined) {
			Inkle.findBoxId(parentId.get)
		} else {
			parentBox.get
		}

		val box = Box.find(boxId).get

		val l: Long = if (parentId.isEmpty) box.maxR + 1 else find(parentId.get)._1.r
		val r: Long = if (parentId.isEmpty) box.maxR + 2 else find(parentId.get)._1.r + 1

		Cypher(
			"""
				update inkle set l = l + 2 where l >= {parentsR} and box_id = {boxId}
			"""
		).on(
			"parentsR" -> l,
			"boxId" -> boxId
		).execute()

		Cypher(
			"""
				update inkle set r = r + 2 where r >= {parentsR} and box_id = {boxId}
			"""
		).on(
			"parentsR" -> l,
			"boxId" -> boxId
		).execute()

		Cypher(
			"""
				insert into inkle values (
					{id}, {createdBy}, {inkle}, {boxId}, {parentId}, {l}, {r}, {created}
				)
			"""
		).on(
			"createdBy" -> inklerId,
			"inkle" -> inkle,
			"boxId" -> boxId,
			"parentId" -> parentId,
			"l" -> l,
			"r" -> r
		).execute()

		if (parentId.isEmpty) {
			Cypher(
				"""
				insert into bump values (
					{id},
					{created}
				)
									"""
			).execute()
		} else if (!isBumped(parentId.get)) {
			Cypher(
				"""
				insert into bump values (
					{id},
					{created}
				)
									"""
			).on(
				"id" -> parentId
			).execute()
		} else {
			Cypher(
				"""
				update bump set created = {created}
				where inkle_id = {parentId}
									"""
			).on(
				"parentId" -> parentId
			).execute()
		}

		Cypher(
			"""
				update box set max_r = max_r + 2
				where id = {boxId}
			"""
		).on(
			"boxId" -> boxId
		).execute()

		// replace with the new inkle's id
		0
	}

	def findAll: Seq[Inkle] = {
		Cypher(
				"""
				select * from inkle
				"""
			).as(simple *)
	}

	def findOops(page: Int = 0, pageSize: Int = 10): Page[(Inkle, Inkler, Box)] = {

		val offset = page * pageSize

		val inkles = Cypher(
			"""
			select inkle.*, inkler.*, box.*
			from (
				(
					(
						bump left join inkle
						on bump.inkle_id = inkle.id
					)
					left join inkler
					on inkle.created_by = inkler.id
				)
				left join box
				on inkle.box_id = box.id
			)
			where box.secret = false
			order by bump.created desc nulls last
			limit {pageSize} offset {offset}
			"""
		).on(
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected *)

		val totalRows = Cypher(
			"""
				select count(*)
				from
				(
					bump left join inkle
					on inkle.id = bump.inkle_id
				)
				left join box
				on box.id = inkle.box_id

				where box.secret = false
			"""
		).as(scalar[Long].single)

		Page(inkles, page, offset, totalRows)
	}

	def findChildren(parentId: Long): Seq[(Inkle, Inkler, Box)] = {
		Cypher(
			"""
				select inkle.*, inkler.*, box.*
				from (
					(
						inkle left join inkler
						on inkle.created_by = inkler.id
					)
					left join box
					on inkle.box_id = box.id
				)
				where inkle.parent_id = {parentId}
				order by inkle.created desc
			"""
		).on(
			"parentId" -> parentId
		).as(withConnected *)
	}

	def getParent(id: Long): (Inkle, Inkler, Box) = {
		Cypher(
			"""
				select inkle.*, inkler.*, box.*
				from
				(
					(
						inkle left join inkler
						on inkle.created_by = inkler.id
					)
					left join box
					on inkle.box_id = box.id
				)
				where inkle.id = {id}
			"""
		).on(
			"id" -> id
		).as(withConnected.single)
	}

	def childrenCount(id: Long): Long = {
		Cypher(
			"""
				select count(inkle.id)
				from inkle
				where inkle.parent_id = {id}
			"""
		).on(
			"id" -> id
		).as(scalar[Long] single)
	}

	def find(id: Long): (Inkle, Inkler, Box) = {
		Cypher(
			"""
				select inkle.*, inkler.*, box.*
				from
				(
					(
						inkle left join inkler
						on inkle.created_by = inkler.id
					)
					left join box
					on inkle.box_id = box.id
				)
				where inkle.id = {id}
			"""
		).on(
			"id" -> id
		).as(withConnected.single)
	}

	def findBoxId(id: Long): Long = {
		Cypher(
			"""
				select inkle.box_id
				from inkle
				where inkle.id = {id}
			"""
		).on(
			"id" -> id
		).as(scalar[Long] single)
	}

	def findFollowed(inklerId: Long, page: Int = 0, pageSize: Int = 10): Page[(Inkle, Inkler, Box)] = {

		val offset = page * pageSize

		val inkles = Cypher(
			"""
			select inkle.*, inkler.*, box.*
			from
			(
				(
					(
						bump left join inkle
						on (inkle.id = bump.inkle_id)
					)
					left join inkler
					on (inkle.created_by = inkler.id)
				)
				left join box
				on (inkle.box_id = box.id)
			)
			left join box_follow
			on (box_follow.box_id = inkle.box_id)

			where inkle.created_by = {inklerId}
			or box_follow.follower_id = {inklerId}

			order by bump.created desc nulls last
			limit {pageSize} offset {offset}
			"""
		).on(
			"inklerId" -> inklerId,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected *)

		val totalRows = Cypher(
			"""
				select count(*)
				from
				(
					bump left join inkle
					on inkle.id = bump.inkle_id
				)
				left join box_follow
				on box_follow.box_id = inkle.box_id

				where inkle.created_by = {inklerId}
				or box_follow.follower_id = {inklerId}
			"""
		).on(
			"inklerId" -> inklerId
		).as(scalar[Long].single)

		Page(inkles, page, offset, totalRows)
	}

	def findByBox(boxId: Long, page: Int = 0, pageSize: Int = 10): Page[(Inkle, Inkler, Box)] = {

		val offset = page * pageSize

		val inkles = Cypher(
			"""
			select inkle.*, inkler.*, box.*
			from (
				(
					(
						bump left join inkle
						on bump.inkle_id = inkle.id
					)
					left join inkler
					on inkle.created_by = inkler.id
				)
				left join box
				on inkle.box_id = box.id
			)
			where inkle.box_id = {boxId}
			order by bump.created desc nulls last
			limit {pageSize} offset {offset}
			"""
		).on(
			"boxId" -> boxId,
			"pageSize" -> pageSize,
			"offset" -> offset
		).as(withConnected *)

		val totalRows = Cypher(
			"""
				select count(*)
				from bump left join inkle
				on inkle.id = bump.inkle_id

				where inkle.box_id = {boxId}
			"""
		).on(
			"boxId" -> boxId
		).as(scalar[Long].single)

		Page(inkles, page, offset, totalRows)
	}

	def view(id: Long): Seq[(Inkle, Inkler, Box)] = {
		Cypher(
			"""
			select inkle.*, inkler.*, box.*
			from
			(
				(
					inkle left join inkler
					on inkle.created_by = inkler.id
				)
				left join box
				on inkle.box_id = box.id
			)
			where inkle.parent_id = {id}
			"""
		).on(
			"id" -> id
		).as(withConnected *)
	}

	def isInBox(inkle: Long, box: Long): Boolean = {
		Cypher(
			"""
			select count(inkle.id) = 1
			from inkle
			where inkle.id = {inkle}
			and inkle.box_id = {box}
			"""
		).on(
			"inkle" -> inkle,
			"box" -> box
		).as(scalar[Boolean].single)
	}

	def isBumped(inkle: Long): Boolean = {
		Cypher(
			"""
				select count(bump.inkle_id) = 1
				from bump
				where bump.inkle_id = {inkle}
			"""
		).on(
			"inkle" -> inkle
		).as(scalar[Boolean].single)
	}
}
