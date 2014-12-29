package security

import javax.swing.Box

import play.api.mvc._
import controllers.routes
import models._


trait Guard {

	private def username(request: RequestHeader) = request.session.get("username")

	private def inkler(request: RequestHeader): Option[Inkler] = {

		val username = request.session.get("username").getOrElse("")

		Inkler.findByUsername(username)
	}

	def user(implicit r: RequestHeader): Inkler = inkler(r).get

	def userOpt(implicit r: RequestHeader): Option[Inkler] = inkler(r)

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
}