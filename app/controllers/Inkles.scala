package controllers

import java.util.UUID

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json._

import models._
import views.html._
import security._
import monkeys.DoLog._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject

object Inkles extends Controller with Guard {

	private def log(log: String, params: Map[String, Any] = Map()) = controllerLogger("Inkles", log, params)

	val inkleForm = Form(
		"inkle" -> text.verifying(
      "Please write something",
      inkle => {
        inkle.trim.nonEmpty
      }
    ).verifying(
      "That's just too long",
      inkle => {
        inkle.trim.length <= 60
      }
    )
	)

	val groupInkleForm = Form(
    tuple(
      "inkle" -> text.verifying(
        "Please write something",
        inkle => {
          inkle.trim.nonEmpty
        }
      ).verifying(
        "That's just too long",
        inkle => {
          inkle.trim.length <= 60
        }
      ),
      "group" -> {
        text.verifying("The group doesn't exist", group => Group.exists(group))
      }
    )
	)

	val editForm = Form(
		"inkle" -> nonEmptyText(maxLength = 60)
	)

	val deleteForm = Form(
    tuple(
      "uuid" -> nonEmptyText(),
      "children" -> nonEmptyText()
    )
	)

	def create(returnAs: String = "rendered") = Action { implicit r =>
		log("create", Map("returnAs" -> returnAs))

    currentUserOpt.map { user =>
      inkleForm.bindFromRequest.fold(
      formWithErrors => BadRequest,
      {
        case (inkle) =>
          val uuid = Inkle.create(user.uuid, inkle.trim)
          val newInkle = Inkle.findFull(uuid)

          if (returnAs == "rendered") {

            Ok(templates.inkles.inkle(currentUser, newInkle))
          } else {

            val json_inkle = obj(
              "uuid" -> newInkle._1.uuid,
              "inkle" -> newInkle._1.inkle,
              "createdTime" -> newInkle._1.created,
              "userUuid" -> newInkle._2.uuid,
              "userUsername" -> newInkle._2.username,
              "childrenCount" -> newInkle._1.childrenCount
            )

            Ok(json_inkle)
          }
      }
      )
    }.getOrElse {
      Redirect(routes.Users.signin())
    }
	}

	def createInGroup(returnAs: String = "rendered") = Action { implicit r =>
		log("createInGroup", Map("returnAs" -> returnAs))

    currentUserOpt.map { user =>
      groupInkleForm.bindFromRequest.fold(
      formWithErrors => BadRequest,
      {
        case (inkle) =>
          val uuid = Inkle.create(user.uuid, inkle._1.trim, groupUuid = Some(inkle._2))
          val newInkle = Inkle.findFull(uuid)

          if (returnAs == "rendered") {

            Ok(templates.inkles.inkle(currentUser, newInkle))
          } else {

            val json_inkle = obj(
              "uuid" -> newInkle._1.uuid,
              "inkle" -> newInkle._1.inkle,
              "createdTime" -> newInkle._1.created,
              "userUuid" -> newInkle._2.uuid,
              "userUsername" -> newInkle._2.username,
              "childrenCount" -> newInkle._1.childrenCount
            )

            Ok(json_inkle)
          }
      }
      )
    }.getOrElse {
      Redirect(routes.Users.signin())
    }
	}

	def extend(parentUuid: String, pageUuid: String, returnAs: String = "rendered") = IsAuthenticated { username => implicit r =>
		log("extend", Map("parentUuid" -> parentUuid))

		val userUuid = User.findUuidByUsername(username).get

		inkleForm.bindFromRequest.fold(
			formWithErrors => BadRequest("something wrong with the data"),
			{
				case (inkle) =>

        val origin = Inkle.getOriginUuid(parentUuid)
				val uuid = Inkle.create(userUuid, inkle, Some(parentUuid), origin)
				val newInkle = Inkle.findFull(uuid)
        val tourStep = User.getTourStep(currentUser.uuid)
        val pathLength = Inkle.getPathLength(uuid)

        if (tourStep < 3 && tourStep < pathLength) {
          User.updateTourStep(currentUser.uuid, pathLength)
        }

        if (returnAs == "rendered") {

          Ok(templates.inkles.child(currentUser, newInkle, pageUuid, UUID.randomUUID().toString, true))
        } else {
          val json_inkle = obj(
            "uuid" -> newInkle._1.uuid,
            "inkle" -> newInkle._1.inkle,
            "createdTime" -> newInkle._1.created,
            "userUuid" -> newInkle._2.uuid,
            "childrenCount" -> newInkle._1.childrenCount,
            "userUsername" -> newInkle._2.username
          )

          Ok(json_inkle)
        }
			}
		)
	}

  def link(from: String, to: String, pageUuid: String) = Action { implicit r =>
    log("link", Map("from" -> from, "to" -> to, "pageUuid" -> pageUuid))


    val link = Inkle.linkShortcut(from, to, currentUser.uuid)

    if (link != "") {
      val newInkle = Inkle.findFull(link)

      Ok(templates.inkles.child(currentUser, newInkle, pageUuid, UUID.randomUUID().toString, isNew = true))
    } else {
      BadRequest("something is up!")
    }

  }

	def edit(inkleUuid: String, pageUuid: String) = IsAuthenticated { username => implicit r =>
		log("edit", Map("inkleUuid" -> inkleUuid))

		editForm.bindFromRequest.fold(
			formWithErrors => BadRequest("something wrong with the data"),
			inkle => {
				val editedInkle = Inkle.edit(inkleUuid, inkle)

        Ok(templates.inkles.central(currentUser, pageUuid, editedInkle))
			}
		)
	}

	def delete = IsAuthenticated { username => implicit r =>
		log("delete")

		deleteForm.bindFromRequest.fold(
			formWithErrors => BadRequest("something wrong with the data"),
			inkle => {
        if (inkle._2 == "delete") {
          if (Inkle.deleteWithChildren(inkle._1)) Ok("deleted")
          else InternalServerError("something went wrong")
        } else {
          if (Inkle.delete(inkle._1)) Ok(templates.inkles.inkle(currentUser, Inkle.findFull(inkle._1)))
          else InternalServerError("something went wrong")
        }

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
          "userUuid" -> inkle._2.uuid,
          "childrenCount" -> inkle._1.childrenCount,
          "userUsername" -> inkle._2.username
        )
      }
    )

    Ok(jsonInkles)
  }

	def getPageOfChildren(uuid: String, pageUuid: String, page: Int = 1) = Action { implicit r =>
    log("getPageOfChildren", Map("uuid" -> uuid, "page" -> page))

    val inkles = Inkle.findPageOfChildren(uuid, page)

    Ok(templates.inkles.children(currentUser, Inkle.findFull(uuid), inkles, pageUuid, page))
  }

	def origin(uuid: String) = IsAuthenticated { username => implicit r =>
		log("origin", Map("uuid" -> uuid))

		Ok(inkle.origin(currentUser, uuid))
	}

	def templateOrigin(uuid: String) = IsAuthenticated { username => implicit r =>
		log("templateOrigin", Map("uuid" -> uuid))

		Ok(templates.origin(currentUser, uuid))
	}

	def templateView(uuid: String) = IsAuthenticated { username => implicit r =>
		log("templateView", Map("uuid" -> uuid))

		Ok(templates.inkles.view(currentUser, uuid))
	}

	def viewInkle(uuid: String) = IsAuthenticated { username => implicit r =>
		log("viewInkle", Map("uuid" -> uuid))

    Redirect(routes.Inkles.view(Inkle.getOrigin(uuid).get._1.uuid, uuid))
	}

	def view(origin: String, uuid: String) = IsAuthenticated { username => implicit r =>
		log("view", Map("origin" -> origin, "uuid" -> uuid))

		Ok(inkle.view(currentUser, uuid))
	}

  /** ajax actions **/

  def fetchPage(page: Int) = IsAuthenticated { username => _ =>
	  log("fetchPage", Map("page" -> page))

    val userUuid = User.findUuidByUsername(username).get

    val inkles = Inkle.findFollowed(userUuid, page)

    val fetchedInkles: JsArray = arr(
      inkles.items.map { inkle =>
        obj(
          "uuid" -> inkle._1.uuid,
          "inkle" -> inkle._1.inkle,
          "parentUuid" -> inkle._1.parentUuid,
          "createdTime" -> inkle._1.created,
          "userUuid" -> inkle._2.uuid,
          "childrenCount" -> inkle._1.childrenCount,
          "userUsername" -> inkle._2.username,
          "children" -> arr(
            Inkle.findChildren(inkle._1.uuid).map { child =>
              obj(
                "uuid" -> child._1.uuid,
                "inkle" -> child._1.inkle,
                "createdTime" -> child._1.created,
                "userUuid" -> child._2.uuid,
                "childrenCount" -> child._1.childrenCount,
                "userUsername" -> child._2.username
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

    val inkles = Inkle.fetchPage(currentUser.uuid, page)

    val fetchedInkles: JsArray = arr(
      inkles.items.map { inkle =>
        obj(
          "uuid" -> inkle._1.uuid,
          "inkle" -> inkle._1.inkle,
          "createdTime" -> inkle._1.created,
          "userUuid" -> inkle._2.uuid,
          "childrenCount" -> inkle._1.childrenCount,
          "userUsername" -> inkle._2.username,
          "children" -> arr(
            Inkle.findChildren(inkle._1.uuid).map { child =>
              obj(
                "uuid" -> child._1.uuid,
                "inkle" -> child._1.inkle,
                "createdTime" -> child._1.created,
                "userUuid" -> child._2.uuid,
                "childrenCount" -> child._1.childrenCount,
                "userUsername" -> child._2.username
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

    val inkle = Inkle.findFull(uuid)

    val fetchedInkles: JsObject = obj(
      "uuid" -> inkle._1.uuid,
      "inkle" -> inkle._1.inkle,
      "parentUuid" -> inkle._1.parentUuid,
      "createdTime" -> inkle._1.created,
      "userUuid" -> inkle._2.uuid,
      "childrenCount" -> inkle._1.childrenCount,
      "userUsername" -> inkle._2.username
    )

    Ok(fetchedInkles)

  }

  def getPath(uuid: String) = Action {
    log("getPath", Map("uuid" -> uuid))

    val path = Inkle.getParents(uuid)

    Ok(templates.inkles.path(path))
  }

  def getInkle(uuid: String) = Action { implicit r =>
	  log("getInkle", Map("uuid" -> uuid))

    val inkle = Inkle.findFull(uuid)


    Ok(templates.inkles.inkle(currentUser, inkle))
  }

  def fetchSuggestions(q: String) = Action { implicit r =>
    log("fetchSuggestions", Map("q" -> q))

    val results = Inkle.suggest(currentUser.uuid, q)

    val jsonResults: JsArray = arr(
      results.items.map { result =>

        obj(
          "uuid"  -> result._1.uuid,
          "inkle" -> result._1.inkle
        )
      }
    )

    Ok(jsonResults)
  }

  def increaseTourStep = Action { implicit r =>
    log("increaseTourStep")

    val tourStep = User.getTourStep(currentUser.uuid)

    if(User.updateTourStep(currentUser.uuid, tourStep + 1)) {
      Ok("tour updated")
    } else {
      InternalServerError("something terrible happened")
    }
  }

  def search(q: String) = Action { implicit r =>
    log("search", Map("q" -> q))

    val results = Inkle.search(q, currentUser.uuid)

    val jsonResults: JsArray = arr(
      results.map { result =>

        val origin = Inkle.getOrigin(result.uuid).get._1

        obj(
          "uuid" -> result.uuid,
          "inkle" -> result.inkle,
          "originUuid" -> origin.uuid,
          "origin" -> origin.inkle
        )
      }
    )

    Ok(jsonResults)
  }

  def searchLinkable(link: String, q: String) = Action { implicit r =>
    log("searchLinkable", Map("link" -> link, "q" -> q))

    val results = if (q.trim != "") {
      Inkle.searchLinkable(q, currentUser.uuid, link)
    } else {
      Inkle.findOrigins(currentUser.uuid, link)
    }

    val jsonResults: JsArray = arr(
      results.map { result =>

        val origin = Inkle.getOrigin(result._1.uuid).get._1

        obj(
          "uuid" -> result._1.uuid,
          "inkle" -> result._1.inkle,
          "originUuid" -> origin.uuid,
          "origin" -> origin.inkle
        )
      }
    )

    Ok(jsonResults)
  }
}
