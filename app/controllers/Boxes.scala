package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._
import security._
import tools.Loggers._

object Boxes extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = controllerLogger("Boxes", log, params)

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
		log("index")

		val inklerId = Inkler.findIdByUsername(username).get

		Ok(html.box.index(Box.findOwnedWithSecret(inklerId), Inkler.find(inklerId).get))
	}

	def create = IsAuthenticated { username => implicit request =>
		log("create")

		val inklerId = Inkler.findIdByUsername(username).get

		boxForm.bindFromRequest.fold(
			formWithErrors => BadRequest,
			{
				case(name, secret) =>
				Box.create(inklerId, name, secret)

				Redirect(request.headers.get(REFERER).get)
			}
		)
	}

	def view(id: Long, page: Int) = IsBoxMember(id) { username => _ =>
		log("view", Map("id" -> id, "page" -> page))

		val inkler = Inkler.findByUsername(username).get

		Box.findWithInkler(id).map { box =>
			Ok(html.box.view(Inkle.findByBox(id, page), Box.members(id), box, inkler))
		}.getOrElse(NotFound)
	}

	def addInkler(boxId: Long, inklerId: Long) = CanAddToBox(boxId, inklerId) { username => implicit request =>
		log("addInkler", Map("boxId" -> boxId, "inklerId" -> inklerId))

		Box.addInkler(boxId, inklerId)
		Redirect(request.headers.get(REFERER).get)
	}

	def removeInkler(boxId: Long, inklerId: Long) = CanRemoveFromBox(boxId, inklerId) { username => implicit request =>
		log("removeInkler", Map("boxId" -> boxId, "inklerId" -> inklerId))

		Box.removeInkler(boxId, inklerId)
		Redirect(request.headers.get(REFERER).get)
	}

	def follows = IsAuthenticated { username => _ =>
		log("follows")

		val inklerId = Inkler.findIdByUsername(username).get

		Ok(html.box.follows(Box.viewFollowed(inklerId), Inkler.find(inklerId).get))
	}

	def follow(boxId: Long) = CanFollowBox(boxId) { username => implicit request =>
		log("follow", Map("boxId" -> boxId))

		val inklerId = Inkler.findIdByUsername(username).get

		Box.follow(boxId, inklerId)
		Redirect(request.headers.get(REFERER).get)
	}

	def unfollow(boxId: Long) = CanUnfollowBox(boxId) { username => implicit request =>
		log("unfollow", Map("boxId" -> boxId))

		val inklerId = Inkler.findIdByUsername(username).get

		Box.unfollow(boxId, inklerId)
		Redirect(request.headers.get(REFERER).get)
	}
}