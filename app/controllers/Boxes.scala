package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._
import security._

object Boxes extends Controller with Guard {

	val boxForm = Form(
		tuple(
			"name"    -> nonEmptyText(maxLength = 25),
			"secret"  -> boolean
		)
	)

	val addInklerForm = Form(
		"inklerId"  -> longNumber
	)

	def index = IsAuthenticated { username => _ =>

		val inklerId = Inkler.findIdByUsername(username).get

		Ok(html.box.index(Box.findOwnedWithSecret(inklerId), Inkler.find(inklerId).get))
	}

	def create = IsAuthenticated { username => implicit request =>

		val inklerId = Inkler.findIdByUsername(username).get
		val maxR: Long = 0

		boxForm.bindFromRequest.fold(
			formWithErrors => BadRequest,
			{
				case(name, secret) =>
				Box.create(inklerId, name, secret, maxR)

				Redirect(request.headers.get(REFERER).get)
			}
		)
	}

	def view(id: Long, page: Int) = IsBoxMember(id) { username => _ =>

		val inkler = Inkler.findByUsername(username).get

		Box.findWithInkler(id).map { box =>
			Ok(html.box.view(Inkle.findByBox(id, page), Box.members(id), box, inkler))
		}.getOrElse(NotFound)
	}

	def addInkler(boxId: Long, inklerId: Long) = CanAddToBox(boxId, inklerId) { username => implicit request =>

		Box.addInkler(boxId, inklerId)
		Redirect(request.headers.get(REFERER).get)
	}

	def removeInkler(boxId: Long, inklerId: Long) = CanRemoveFromBox(boxId, inklerId) { username => implicit request =>

		Box.removeInkler(boxId, inklerId)
		Redirect(request.headers.get(REFERER).get)
	}

	def follows = IsAuthenticated { username => _ =>

		val inklerId = Inkler.findIdByUsername(username).get

		Ok(html.box.follows(Box.viewFollowed(inklerId), Inkler.find(inklerId).get))
	}

	def follow(boxId: Long) = CanFollowBox(boxId) { username => implicit request =>

		val inklerId = Inkler.findIdByUsername(username).get

		Box.follow(boxId, inklerId)
		Redirect(request.headers.get(REFERER).get)
	}

	def unfollow(boxId: Long) = CanUnfollowBox(boxId) { username => implicit request =>

		val inklerId = Inkler.findIdByUsername(username).get

		Box.unfollow(boxId, inklerId)
		Redirect(request.headers.get(REFERER).get)
	}
}