package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import views.html

import security._
import monkeys.DoLog._
import models._

object Groups extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		controllerLogger("Groups", log, params)
	}

	val groupForm = Form(
		"name" -> nonEmptyText(maxLength = 35)
	)

	def list = Action { implicit r =>
		log("list")

		val groups = Group.findOwned(currentUser.uuid)
		Ok(html.groups.list(currentUser, groups))
	}

	def create = Action { implicit r =>
		log("create")

		groupForm.bindFromRequest.fold(
			error => BadRequest("this is called an error!"),
			group => {
				Group.create(group, currentUser.uuid)

				Ok("created")
			}
		)
	}

	def view(uuid: String) = Action { implicit r =>
		log("view", Map("uuid" -> uuid))

		val groupOpt = Group.find(uuid)

		groupOpt.map { group =>
			Ok(html.groups.view(currentUser, group))
		}.getOrElse {
			NotFound("Not found")
		}
	}

	// templates

	def templateList = Action { implicit r =>
		log("templateList")

		val groups = Group.findOwned(currentUser.uuid)
		Ok(html.templates.groups.list(currentUser, groups))
	}

	def templateView(uuid: String) = Action { implicit r =>
		log("templateView", Map("uuid" -> uuid))

		val groupOpt = Group.find(uuid)

		groupOpt.map { group =>
			Ok(html.templates.groups.view(currentUser, group))
		}.getOrElse {
			NotFound("Not found")
		}
	}
}