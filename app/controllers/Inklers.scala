package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import views._
import security._
import tools.Loggers._

object Inklers extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = controllerLogger("Inklers", log, params)

	val signupForm = Form(
		tuple(
			"username"        -> nonEmptyText(maxLength = 12).verifying("The username is taken", username => !Inkler.usernameExists(username)),
			"firstName"       -> nonEmptyText,
			"lastName"        -> nonEmptyText,
			"email"           -> email.verifying("The email address is already in use", email => !Inkler.emailExists(email)),
			"password"        -> text(minLength = 8)
		)
	)

	val signinForm = Form(
		tuple(
			"usernameOrEmail" -> nonEmptyText,
			"password"        -> nonEmptyText
		) verifying("Incorrect username/email or password", result => result match {
			case (usernameOrEmail, password) => Inkler.authenticate(usernameOrEmail.toLowerCase, password).isDefined
		})
	)

	def signup = Action {
		log("signup")

		Ok(html.inkler.signup(signupForm))
	}

	def create = Action { implicit request =>
		log("create")

		signupForm.bindFromRequest.fold(
			formWithErrors => BadRequest(html.inkler.signup(formWithErrors)),
			inkler => {
				Inkler.create(inkler._1, inkler._2, inkler._3, inkler._4, inkler._5)
				Redirect(routes.Apps.home()).withSession("username" -> inkler._1)
			}
		)
	}

	def view(id: Long) = IsAuthenticated { username => _ =>
		log("view")

		Inkler.findUsernameById(id).map { viewedUsername =>
			Redirect(routes.Inklers.viewByUsername(viewedUsername))
		}.getOrElse(NotFound)
	}

	def viewByUsername(viewedInkler: String) = IsAuthenticated { username => _ =>
		log("viewByUsername", Map("viewedInkler" -> viewedInkler))

		Inkler.findIdByUsername(viewedInkler).map { viewedInklerId =>
			val currentInklerId = Inkler.findIdByUsername(username).get

			val inkler: Inkler = Inkler.find(viewedInklerId).get
			val currentInkler: Inkler = Inkler.find(currentInklerId).get

			if (currentInklerId == viewedInklerId) {
				Ok(
					html.inkler.view(
						inkler,
						currentInkler,
						Box.findOwned(viewedInklerId),
						Box.findSecret(viewedInklerId),
						Box.findSecret(viewedInklerId),
						Box.findSecret(viewedInklerId)
					)
				)
			} else {
				Ok(
					html.inkler.view(
						inkler,
						currentInkler,
						Box.findOwned(viewedInklerId),
						Box.findNonInvitedByOwner(currentInklerId, viewedInklerId),
						Box.findInvitedByOwner(currentInklerId, viewedInklerId),
						Box.findInvitedByOwner(viewedInklerId, currentInklerId)
					)
				)
			}
		}.getOrElse(NotFound)
	}

	def signin = Action {
		log("signin")

		Ok(html.inkler.signin(signinForm))
	}

	def signout = Action {
		log("signout")

		Redirect(routes.Apps.home()).withNewSession
	}

	def authenticate = Action { implicit request =>
		log("authenticate")

    signinForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.inkler.signin(formWithErrors)),
      inkler => Redirect(routes.Apps.home()).withSession("username" -> Inkler.findUsernameByUsernameOrEmail(inkler._1.toLowerCase))
    )
  }
}
