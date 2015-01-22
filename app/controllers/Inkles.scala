package controllers

import java.util.UUID

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json._

import models._
import views.html._
import security._
import monkeys.Loggers._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject

object Inkles extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = controllerLogger("Inkles", log, params)

	val inkleForm = Form(
		"inkle" -> nonEmptyText(maxLength = 80)
	)

	val editForm = Form(
		"inkle" -> nonEmptyText(maxLength = 80)
	)

	def create(returnAs: String = "rendered") = Action { implicit request =>
		log("create")

    userOpt.map { inkler =>
      inkleForm.bindFromRequest.fold(
      formWithErrors => BadRequest,
      {
        case (inkle) =>
          val uuid = Inkle.create(inkler.uuid, None, inkle)
          val newInkle = Inkle.find(uuid)

          if (returnAs == "rendered") {

            Ok(renderers.inkles.inkle(newInkle))
          } else {

            val json_inkle = obj(
              "uuid" -> newInkle._1.uuid,
              "inkle" -> newInkle._1.inkle,
              "createdTime" -> newInkle._1.created,
              "inklerUuid" -> newInkle._2.uuid,
              "inklerUsername" -> newInkle._2.username,
              "childrenCount" -> Inkle.childrenCount(newInkle._1.uuid)
            )

            Ok(json_inkle)
          }
      }
      )
    }.getOrElse {
      Redirect(routes.Inklers.signin())
    }
	}

	def extend(parentUuid: String, pageUuid: String, returnAs: String = "rendered") = IsAuthenticated { username => implicit request =>
		log("extend", Map("parentUuid" -> parentUuid))

		val inklerUuid = Inkler.findUuidByUsername(username).get

		inkleForm.bindFromRequest.fold(
			formWithErrors => BadRequest("something wrong with the data"),
			{
				case (inkle) =>
				val uuid = Inkle.create(inklerUuid, Some(parentUuid), inkle)
				val newInkle = Inkle.find(uuid)

        if (returnAs == "rendered") {

          Ok(renderers.inkles.extend(newInkle, pageUuid, UUID.randomUUID().toString))
        } else {
          val json_inkle = obj(
            "uuid" -> newInkle._1.uuid,
            "inkle" -> newInkle._1.inkle,
            "createdTime" -> newInkle._1.created,
            "inklerUuid" -> newInkle._2.uuid,
            "childrenCount" -> Inkle.childrenCount(newInkle._1.uuid),
            "inklerUsername" -> newInkle._2.username
          )

          Ok(json_inkle)
        }
			}
		)
	}

	def edit(inkleUuid: String, pageUuid: String) = IsAuthenticated { username => implicit request =>
		log("edit", Map("inkleUuid" -> inkleUuid))

		editForm.bindFromRequest.fold(
			formWithErrors => BadRequest("something wrong with the data"),
			inkle => {
				val editedInkle = Inkle.edit(inkleUuid, inkle)

        Ok(renderers.inkles.central(pageUuid, editedInkle))
			}
		)
	}

	def getChildren(uuid: String) = Action {
    log("getChildren", Map("uuid" -> uuid))

    val inkles = Inkle.findChildren(uuid)

    val jsonInkles: JsArray = arr(
      inkles.map { inkle =>
        obj(
          "uuid" -> inkle._1.uuid,
          "inkle" -> inkle._1.inkle,
          "createdTime" -> inkle._1.created,
          "inklerUuid" -> inkle._2.uuid,
          "childrenCount" -> Inkle.childrenCount(inkle._1.uuid),
          "inklerUsername" -> inkle._2.username
        )
      }
    )

    Ok(jsonInkles)
  }

	def view(uuid: String) = IsAuthenticated { username => _ =>
		log("view", Map("uuid" -> uuid))

		Ok(
			inkle.view()
		)
	}


  /** ajax actions **/

  def fetchPage(page: Int) = IsAuthenticated { username => _ =>
	  log("fetchPage", Map("page" -> page))

    val inklerUuid = Inkler.findUuidByUsername(username).get

    val inkles = Inkle.findFollowed(inklerUuid, page)

    val fetchedInkles: JsArray = arr(
      inkles.items.map { inkle =>
        obj(
          "uuid" -> inkle._1.uuid,
          "inkle" -> inkle._1.inkle,
          "parentUuid" -> inkle._1.parentUuid,
          "createdTime" -> inkle._1.created,
          "inklerUuid" -> inkle._2.uuid,
          "childrenCount" -> Inkle.childrenCount(inkle._1.uuid),
          "inklerUsername" -> inkle._2.username,
          "children" -> arr(
            Inkle.findChildren(inkle._1.uuid).map { child =>
              obj(
                "uuid" -> child._1.uuid,
                "inkle" -> child._1.inkle,
                "createdTime" -> child._1.created,
                "inklerUuid" -> child._2.uuid,
                "childrenCount" -> Inkle.childrenCount(child._1.uuid),
                "inklerUsername" -> child._2.username
              )
            }
          )
        )
      }
    )

    Ok(fetchedInkles)
  }

  def fetchOopsPage(page: Int) = Action { implicit r =>
	  log("fetchOopsPage", Map("page" -> page))

    val inkles = Inkle.fetchPage(user.uuid, page)

    val fetchedInkles: JsArray = arr(
      inkles.items.map { inkle =>
        obj(
          "uuid" -> inkle._1.uuid,
          "inkle" -> inkle._1.inkle,
          "createdTime" -> inkle._1.created,
          "inklerUuid" -> inkle._2.uuid,
          "childrenCount" -> Inkle.childrenCount(inkle._1.uuid),
          "inklerUsername" -> inkle._2.username,
          "children" -> arr(
            Inkle.findChildren(inkle._1.uuid).map { child =>
              obj(
                "uuid" -> child._1.uuid,
                "inkle" -> child._1.inkle,
                "createdTime" -> child._1.created,
                "inklerUuid" -> child._2.uuid,
                "childrenCount" -> Inkle.childrenCount(child._1.uuid),
                "inklerUsername" -> child._2.username
              )
            }
          )
        )
      }
    )

    Ok(fetchedInkles)
  }

  def getParent(uuid: String) = Action {
	  log("getParent", Map("uuid" -> uuid))

    val inkle = Inkle.find(uuid)

    val fetchedInkles: JsObject = obj(
      "uuid" -> inkle._1.uuid,
      "inkle" -> inkle._1.inkle,
      "parentUuid" -> inkle._1.parentUuid,
      "createdTime" -> inkle._1.created,
      "inklerUuid" -> inkle._2.uuid,
      "childrenCount" -> Inkle.childrenCount(inkle._1.uuid),
      "inklerUsername" -> inkle._2.username
    )

    Ok(fetchedInkles)

  }

  def getInkle(uuid: String) = Action {
	  log("getInkle", Map("uuid" -> uuid))

    val inkle = Inkle.find(uuid)


    Ok(renderers.inkles.inkle(inkle))

  }
}
