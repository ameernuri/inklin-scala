package security

import play.api.mvc._
import controllers.routes
import models._

trait Guard {

	private def username(request: RequestHeader) = request.session.get("username")

	private def user(request: RequestHeader): Option[User] = {

		val username = request.session.get("username").getOrElse("")

		User.findByUsername(username)
	}

	def currentUser(implicit r: RequestHeader): User = user(r).get

	def currentUserOpt(implicit r: RequestHeader): Option[User] = user(r)

	private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Apps.home())

	def IsAuthenticated(f: String => Request[AnyContent] => Result) = Security.Authenticated(
		username, onUnauthorized
	) { user =>

		if (!User.usernameExists(user)) {
			Action(Results.Redirect(routes.Users.signin()).withNewSession)
		} else {
			Action(request => f(user)(request))
		}
	}
}