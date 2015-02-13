package controllers

import controllers.Inkles._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.JsArray
import play.api.libs.json.Json._
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
		tuple(
			"name" -> nonEmptyText(maxLength = 35),
			"description" -> nonEmptyText(maxLength = 70)
		)
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

				val newGroup = Group.create(group._1, group._2, currentUser.uuid)

				newGroup.map { group =>
					val jsonGroup = obj(
						"uuid" -> group.uuid,
						"name" -> group.name,
						"description" -> group.description,
						"admin" -> group.admin
					)

					Ok(jsonGroup)
				}.getOrElse {
					BadRequest("group not found")
				}
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