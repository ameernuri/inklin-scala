package security

import play.api.mvc._
import controllers.routes
import models._


trait Guard {

	private def username(request: RequestHeader) = request.session.get("username")

	private def inkler(request: RequestHeader): Option[Inkler] = {

		val username = request.session.get("username").getOrElse("")

		//Inkler.findByUsername(username)
		None
	}

	implicit def user(implicit r: RequestHeader): Inkler = inkler(r).get

	implicit def userOpt(implicit r: RequestHeader): Option[Inkler] = inkler(r)

	private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Apps.home())

	def IsAuthenticated(f: String => Request[AnyContent] => Result) = Security.Authenticated(
		username, onUnauthorized
	) { inkler =>

		if (!Inkler.usernameExists(inkler)) {
			Action(Results.Redirect(routes.Inklers.signin()).withNewSession)
		} else {
			Action(request => f(inkler)(request))
		}
	}

	def IsBoxMember(box: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { inkler => request =>

		val inklerId = Inkler.findIdByUsername(inkler).get

		if (Box.isSecret(box)) {
			if (Box.isOwner(box, inklerId) || Box.isMember(box, inklerId)) {
				f(inkler)(request)
			} else {
				Results.Forbidden
			}
		} else {
			f(inkler)(request)
		}
	}

	def IsBoxOwner(box: Long)
		(f: => String => Request[AnyContent] => Result) = IsAuthenticated { inkler => request =>

		val inklerId = Inkler.findIdByUsername(inkler).get

		if (Box.isOwner(box, inklerId)) {
			f(inkler)(request)
		} else {
			Results.Forbidden
		}
	}

	def CanAddToBox(box: Long, addedInklerId: Long)
		(f: => String => Request[AnyContent] => Result) = IsAuthenticated { inkler => request =>

		val inklerId = Inkler.findIdByUsername(inkler).get

		if (
			Box.isOwner(box, addedInklerId)
			|| Box.isMember(box, addedInklerId)
			|| !Box.isOwner(box, inklerId)
			|| !Box.isSecret(box)
		) {
			Results.BadRequest
		} else {
			f(inkler)(request)
		}
	}

	def CanRemoveFromBox(box: Long, removedInklerId: Long)
		(f: => String => Request[AnyContent] => Result) = IsAuthenticated { inkler => request =>

		val inklerId = Inkler.findIdByUsername(inkler).get

		if (
			Box.isOwner(box, removedInklerId)
			|| !Box.isMember(box, removedInklerId)
			|| !Box.isOwner(box, inklerId)
		) {
			Results.BadRequest
		} else {
			f(inkler)(request)
		}
	}

	def CanFollowBox(box: Long)
		(f: => String => Request[AnyContent] => Result) = IsAuthenticated { inkler => request =>

		val inklerId = Inkler.findIdByUsername(inkler).get

		if (Box.isOwner(box, inklerId) || Box.hasFollowed(box, inklerId)) {
			Results.BadRequest
		} else {
			f(inkler)(request)
		}
	}

	def CanUnfollowBox(box: Long)
		(f: => String => Request[AnyContent] => Result) = IsAuthenticated { inkler => request =>

		val inklerId = Inkler.findIdByUsername(inkler).get

		if (Box.hasFollowed(box, inklerId)) {
			f(inkler)(request)
		} else {
			Results.BadRequest
		}
	}

	def CanCreate(box: Long)
		(f: => String => Request[AnyContent] => Result) = IsAuthenticated { inkler => request =>

		val inklerId = Inkler.findIdByUsername(inkler).get

		if (Box.isSecret(box)) {
			if (Box.isOwner(box, inklerId) || Box.isMember(box, inklerId)) {
				f(inkler)(request)
			} else {
				Results.BadRequest
			}
		} else {
			f(inkler)(request)
		}
	}

	def CanExtend(inkle: Long)
		(f: => String => Request[AnyContent] => Result) = IsAuthenticated { inkler => request =>

		val inklerId = Inkler.findIdByUsername(inkler).get
		val box = Inkle.findBoxId(inkle)

		if (Box.isSecret(box)) {
			if (Box.isOwner(box, inklerId) || Box.isMember(box, inklerId)) {
				f(inkler)(request)
			} else {
				Results.BadRequest
			}
		} else {
			f(inkler)(request)
		}
	}
}