package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import views._
import security._
import monkeys.Loggers._

object Inklers extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = controllerLogger("Inklers", log, params)

	val signupForm = Form(
		tuple(
			"username"        -> nonEmptyText(maxLength = 12).verifying("The username is taken", username => !Inkler.usernameExists(username)),
			"name"        -> nonEmptyText,
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
				Inkler.create(inkler._1, inkler._2, inkler._3, inkler._4)
				Redirect(routes.Apps.home()).withSession("username" -> inkler._1)
			}
		)
	}

	def view(uuid: String) = IsAuthenticated { username => _ =>
		log("view")

		Inkler.findUsernameByUuid(uuid).map { viewedUsername =>
			Redirect(routes.Inklers.viewByUsername(viewedUsername))
		}.getOrElse(NotFound)
	}

	def viewByUsername(viewedInkler: String) = IsAuthenticated { username => _ =>
		log("viewByUsername", Map("viewedInkler" -> viewedInkler))

		Inkler.findUuidByUsername(viewedInkler).map { viewedInklerUuid =>
			val currentInklerUuid = Inkler.findUuidByUsername(username).get

			val inkler: Inkler = Inkler.find(viewedInklerUuid).get
			val currentInkler: Inkler = Inkler.find(currentInklerUuid).get

			if (currentInklerUuid == viewedInklerUuid) {
				Ok(
					html.inkler.view()
				)
			} else {
				Ok(
					html.inkler.view()
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
