package controllers

import play.api.mvc._

import models._
import views._

import security._
import monkeys.Loggers._

object Apps extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		controllerLogger("Apps", log, params)
	}


	def home = Action { implicit r =>
		log("home")

		Ok(html.home(currentUserOpt))
	}

	def origins = Action { implicit r =>
		log("origins")

		Ok(html.origins(currentUserOpt))
	}

	def inkle = Action { implicit r =>
		log("inkle")

		Ok(html.inkle.inkle())
	}
}