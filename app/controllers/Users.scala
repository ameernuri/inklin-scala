package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import views._
import security._
import monkeys.Loggers._

object Users extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = controllerLogger("Users", log, params)

	val signupForm = Form(
		tuple(
			"username"        -> nonEmptyText(maxLength = 12).verifying("The username is taken", username => !User.usernameExists(username)),
			"name"        -> nonEmptyText,
			"email"           -> email.verifying("The email address is already in use", email => !User.emailExists(email)),
			"password"        -> text(minLength = 8)
		)
	)

	val signinForm = Form(
		tuple(
			"usernameOrEmail" -> nonEmptyText,
			"password"        -> nonEmptyText
		) verifying("Incorrect username/email or password", result => result match {
			case (usernameOrEmail, password) => User.authenticate(usernameOrEmail.toLowerCase, password).isDefined
		})
	)

	def signup = Action {
		log("signup")

		Ok(html.user.signup(signupForm))
	}

	def create = Action { implicit request =>
		log("create")

		signupForm.bindFromRequest.fold(
			formWithErrors => BadRequest(html.user.signup(formWithErrors)),
			user => {
				User.create(user._1, user._2, user._3, user._4)
				Redirect(routes.Apps.home()).withSession("username" -> user._1)
			}
		)
	}

	def view(uuid: String) = IsAuthenticated { username => _ =>
		log("view")

		User.findUsernameByUuid(uuid).map { viewedUsername =>
			Redirect(routes.Users.viewByUsername(viewedUsername))
		}.getOrElse(NotFound)
	}

	def viewByUsername(viewedUser: String) = IsAuthenticated { username => _ =>
		log("viewByUsername", Map("viewedUser" -> viewedUser))

		User.findUuidByUsername(viewedUser).map { viewedUserUuid =>
			val currentUserUuid = User.findUuidByUsername(username).get

			val user: User = User.find(viewedUserUuid).get
			val currentUser: User = User.find(currentUserUuid).get

			if (currentUserUuid == viewedUserUuid) {
				Ok(
					html.user.view(currentUser, user)
				)
			} else {
				Ok(
					html.user.view(currentUser, user)
				)
			}
		}.getOrElse(NotFound)
	}

	def templateView(username: String) = Action {

		User.findUuidByUsername(username).map { viewedUserUuid =>

			val user: User = User.find(viewedUserUuid).get

			Ok(
				html.templates.users.view(user)
			)
		}.getOrElse(NotFound)
	}

	def signin = Action {
		log("signin")

		Ok(html.user.signin(signinForm))
	}

	def signout = Action {
		log("signout")

		Redirect(routes.Apps.home()).withNewSession
	}

	def authenticate = Action { implicit request =>
		log("authenticate")

    signinForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.user.signin(formWithErrors)),
      user => Redirect(routes.Apps.home()).withSession("username" -> User.findUsernameByUsernameOrEmail(user._1.toLowerCase))
    )
  }
}
