package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import views._
import security._

object Inklers extends Controller with Guard {

	val signupForm = Form(
		tuple(
			"username"        -> nonEmptyText(maxLength = 12).verifying("The username is taken", username => !Inkler.usernameExists(username)),
			"firstName"       -> nonEmptyText,
			"lastName"        -> nonEmptyText,
			"email"           -> email.verifying("The email address is already in use", email => !Inkler.emailExists(email)),
			"password"        -> text(minLength = 8),
			"invitationCode"  -> nonEmptyText
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

		Ok(html.inkler.signup(signupForm))
	}

	def create = Action { implicit request =>

		signupForm.bindFromRequest.fold(
			formWithErrors => BadRequest(html.inkler.signup(formWithErrors)),
			inkler => {
				Inkler.create(inkler._1, inkler._2, inkler._3, inkler._4, inkler._5, inkler._6)
				Redirect(routes.Apps.home()).withSession("username" -> inkler._1)
			}
		)
	}

	def view(id: Long) = IsAuthenticated { username => _ =>

		Inkler.findUsernameById(id).map { viewedUsername =>
			Redirect(routes.Inklers.viewByUsername(viewedUsername))
		}.getOrElse(NotFound)
	}

	def viewByUsername(viewedInkler: String) = IsAuthenticated { username => _ =>

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
						Box.findNotMemberedByCreator(currentInklerId, viewedInklerId),
						Box.findMemberedByCreator(currentInklerId, viewedInklerId),
						Box.findMemberedByCreator(viewedInklerId, currentInklerId)
					)
				)
			}
		}.getOrElse(NotFound)
	}

	def signin = Action {

		Ok(html.inkler.signin(signinForm))
	}

	def signout = Action {

		Redirect(routes.Apps.home()).withNewSession
	}

	def authenticate = Action { implicit request =>

    signinForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.inkler.signin(formWithErrors)),
      inkler => Redirect(routes.Apps.home()).withSession("username" -> Inkler.findUsernameByUsernameOrEmail(inkler._1.toLowerCase))
    )
  }
}
