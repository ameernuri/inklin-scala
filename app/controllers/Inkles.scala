package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json._

import models._
import views._
import security._
import monkeys.Loggers._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import scala.Some

object Inkles extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = controllerLogger("Inkles", log, params)

	val inkleForm = Form(
		"inkle" -> nonEmptyText(maxLength = 70)
	)

	def create = Action { implicit request =>
		log("create")

    userOpt.map { inkler =>
      inkleForm.bindFromRequest.fold(
      formWithErrors => BadRequest,
      {
        case (inkle) =>
          val uuid = Inkle.create(inkler.uuid, None, inkle)
          val new_inkle = Inkle.find(uuid)

          val json_inkle = obj(
            "uuid" -> new_inkle._1.uuid,
            "inkle" -> new_inkle._1.inkle,
            "createdTime" -> new_inkle._1.created,
            "inklerUuid" -> new_inkle._2.uuid,
            "inklerUsername" -> new_inkle._2.username,
            "childrenCount" -> Inkle.childrenCount(new_inkle._1.uuid)
          )

          Ok(json_inkle)
      }
      )
    }.getOrElse {
      Redirect(routes.Inklers.signin())
    }
	}

	def extend(parentUuid: String) = IsAuthenticated { username => implicit request =>
		log("extend", Map("parentUuid" -> parentUuid))

		val inklerUuid = Inkler.findUuidByUsername(username).get

		inkleForm.bindFromRequest.fold(
			formWithErrors => Forbidden,
			{
				case (inkle) =>
				val uuid = Inkle.create(inklerUuid, Some(parentUuid), inkle)
				val new_inkle = Inkle.find(uuid)

				val json_inkle = obj(
					"uuid" -> new_inkle._1.uuid,
					"inkle" -> new_inkle._1.inkle,
					"createdTime" -> new_inkle._1.created,
					"inklerUuid" -> new_inkle._2.uuid,
					"childrenCount" -> Inkle.childrenCount(new_inkle._1.uuid),
					"inklerUsername" -> new_inkle._2.username
				)

				Ok(json_inkle)
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

	def getInkle(uuid: String) = Action {
		log("getInkle", Map("uuid" -> uuid))

		val inkle = Inkle.find(uuid)
		Ok(inkle._1.inkle)
	}

	def view(uuid: String) = IsAuthenticated { username => _ =>
		log("view", Map("uuid" -> uuid))

		Ok(
			html.inkle.view()
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

  def fetchOopsPage(page: Int) = Action {
	  log("fetchOopsPage", Map("page" -> page))

    val inkles = Inkle.fetchPage(page)

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
}
