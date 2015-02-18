package controllers

import play.api.Routes
import play.api.mvc._
import views.html._
import views.html

import security._
import monkeys.DoLog._

import controllers.routes.javascript

object Apps extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		controllerLogger("Apps", log, params)
	}


	def home = Action { implicit r =>
		log("home")

		Ok(html.home(currentUserOpt))
	}

	def templateHome = Action { implicit r =>
		log("templateHome")

		Ok(templates.home(currentUserOpt))
	}

	def templateOrigins = Action { implicit r =>
		log("templateOrigins")

		Ok(templates.origins(currentUser))
	}

	def origins = Action { implicit r =>
		log("origins")

		Ok(html.origins(currentUserOpt))
	}

	def inkle = Action { implicit r =>
		log("inkle")

		Ok(html.inkle.inkle(currentUser))
	}

	def templateTest = Action { implicit r =>
		Ok(html.mails.resetPassword("ameer@inklin.co", "123456"))
	}

	// js router
	def javascriptRoutes = Action { implicit request =>
	  Ok(
	    Routes.javascriptRouter("jsRoutes")(
	      routes.javascript.Apps.templateHome,
	      routes.javascript.Apps.templateOrigins,
	      routes.javascript.Inkles.templateOrigin,
	      routes.javascript.Inkles.create,
	      routes.javascript.Inkles.createInGroup,
	      routes.javascript.Inkles.edit,
	      routes.javascript.Inkles.delete,
	      routes.javascript.Inkles.extend,
	      routes.javascript.Inkles.getInkle,
	      routes.javascript.Inkles.getPageOfChildren,
	      routes.javascript.Inkles.fetchSuggestions,
	      routes.javascript.Users.templateView,
		    routes.javascript.Groups.create,
		    routes.javascript.Groups.update,
		    routes.javascript.Groups.templateList,
		    routes.javascript.Groups.templateView
	  )
	  ).as("text/javascript")
	}
}