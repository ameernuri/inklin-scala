package controllers

import org.apache.commons.lang3.RandomStringUtils._
import play.api.libs.Crypto._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import play.twirl.api.Html
import views._
import security._
import monkeys.DoLog._
import play.twirl.api.Html

object Users extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = controllerLogger("Users", log, params)

	val signupForm = Form(
		tuple(
			"username" -> nonEmptyText(maxLength = 12).verifying("The username is taken", username => !User.usernameExists(username)),
			"name"        -> nonEmptyText,
			"email" -> {
				email.verifying("The email address is already in use", email => !User.emailExists(email.toLowerCase.trim))
			},
			"password" -> text(minLength = 8)
		)
	)

	val signinForm = Form(
		tuple(
			"usernameOrEmail" -> nonEmptyText,
			"password"        -> nonEmptyText
		).verifying("Incorrect username/email or password", result => result match {
			case (usernameOrEmail, password) => User.authenticate(usernameOrEmail.toLowerCase, encryptAES(password)).isDefined
		})
	)

	val resetForm = Form(
		"usernameOrEmail" -> nonEmptyText.verifying(
			"Inklin couldn't find the username/email you entered. Please try again",
			result => User.usernameOrEmailExists(result.toLowerCase)
		)
	)

	val resetPasswordProcessForm = Form(
		"passwords" -> tuple(
			"password" -> nonEmptyText(minLength = 8),
			"confirm" -> nonEmptyText
		).verifying(
		  "Passwords don't match",
		  passwords => passwords._1 == passwords._2
		)
	)

	def signup = Action { implicit r =>
		log("signup")

		Ok(html.user.signup(signupForm))
	}

	def create = Action { implicit r =>
		log("create")

		signupForm.bindFromRequest.fold(
			formWithErrors => BadRequest(html.user.signup(formWithErrors)),
			user => {
				User.create(user._1, user._2, user._3, encryptAES(user._4))
				Redirect(routes.Apps.home()).withSession("username" -> user._1)
			}
		)
	}

	def resetPassword = Action { implicit r =>
		log("resetPassword")

		val resetCode = randomAlphanumeric(30)

		resetForm.bindFromRequest.fold(
			formWithErrors => BadRequest(html.user.resetPassword(formWithErrors)),
			usernameOrEmail => {

				val user = User.findByUsernameOrEmail(usernameOrEmail.toLowerCase).get

				if (User.createResetPasswordCode(user.uuid, resetCode)) {

					monkeys.DoMail.sendHtml(
						"Reset your password",
						html.mails.resetPassword(user.email, resetCode),
						"ameernuri@gmail.com"
					)

					Redirect(routes.Apps.home()).flashing(
						"success" -> s"Instructions to reset your password sent to ${user.email}"
					)

				} else {
					Redirect(routes.Users.resetPasswordPage()).flashing(
						"error" -> "Couldn't send reset instructions. Please try again in a while"
					)
				}
			}
		)
	}

	def resetPasswordProcessPage(email: String, code: String) = Action { implicit r =>
		log("resetPasswordProcessPage", Map("email" -> email, "code" -> code))

		Ok(html.user.resetPasswordProcess(resetPasswordProcessForm, email, code)).withNewSession
	}

	def resetPasswordProcess(email: String, code: String) = Action { implicit r =>
		log("resetPasswordProcess", Map("email" -> email, "code" -> code))

		resetPasswordProcessForm.bindFromRequest.fold(
			errors => BadRequest(html.user.resetPasswordProcess(errors, email, code)),
			passwords => {

				val userOpt = User.findByEmail(email.toLowerCase)

				userOpt.map { user =>
					if (User.validatePasswordResetCode(email: String, code: String)) {
						if (User.updatePassword(user.uuid, encryptAES(passwords._1))) {
							Redirect(routes.Users.signin()).flashing(
								"success" -> "Your password has been updated"
							)
						} else {
							BadRequest(html.user.resetPasswordProcess(resetPasswordProcessForm, email, code)).flashing(
								"error" -> "Oops, something happened while updating your password. Please try again in a while"
							)
						}
					} else {
						Redirect(routes.Apps.home()).flashing(
							"error" -> "Invalid or expired password reset code"
						)
					}
				}.getOrElse {
					Redirect(routes.Users.resetPasswordPage()).flashing(
						"error" -> "Something went wrong."
					)
				}
			}
		)
	}

	def resetPasswordPage = Action { implicit request =>
		log("resetPasswordPage")

		Ok(html.user.resetPassword(resetForm))
	}

	def view(uuid: String) = IsAuthenticated { username => _ =>
		log("view")

		User.findUsernameByUuid(uuid).map { viewedUsername =>
			Redirect(routes.Users.viewByUsername(viewedUsername))
		}.getOrElse(NotFound)
	}

	def viewByUsername(viewedUser: String) = IsAuthenticated { username => implicit r =>
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

	def signin = Action { implicit r =>
		log("signin")

		Ok(html.user.signin(signinForm))
	}

	def signout = Action { implicit r =>
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
