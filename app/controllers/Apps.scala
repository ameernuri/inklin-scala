package controllers

import play.api.mvc._

import models._
import views._

import security._
import tools.Loggers._

object Apps extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		controllerLogger("Apps", log, params)
	}
  
  def home = Action { implicit r =>
	  log("home")

	  if (userOpt.isDefined) {

		  val inklerId = Inkler.findIdByUsername("username").get
		  val inkler = Inkler.find(inklerId).get

		  val inkles = Inkle.findFollowed(inklerId)

		  Ok(
			  html.index(
				  inkles,
				  Box.findOwned(inklerId),
				  Box.findSecret(inklerId),
				  Box.findMembered(inklerId),
				  Box.findFollowed(inklerId),
				  inkler
			  )
		  )

	  } else {
		  Ok(html.landing(Inklers.signinForm, Inklers.signupForm))
	  }
  }
}