package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json._

import models._
import views._
import security._

object Inkles extends Controller with Guard {

	val inkleForm = Form(
		"inkle" -> nonEmptyText(maxLength = 70)
	)

	def create(boxId: Long) = CanCreate(boxId) { username => implicit request =>

		val inklerId = Inkler.findIdByUsername(username).get

		inkleForm.bindFromRequest.fold(
			formWithErrors => BadRequest,
			{
				case (inkle) =>
				val id = Inkle.create(inklerId, Some(boxId), None, inkle)
				val new_inkle = Inkle.find(id)

				val json_inkle = obj(
					"id"              -> new_inkle._1.id,
					"inkle"           -> new_inkle._1.inkle,
					"createdTime"     -> new_inkle._1.created,
					"inklerId"        -> new_inkle._2.id,
					"inklerUsername"  -> new_inkle._2.username,
          "childrenCount"   -> Inkle.childrenCount(new_inkle._1.id),
          "boxOwner"        -> Inkler.findUsernameById(new_inkle._3.createdBy),
          "boxName"         -> new_inkle._3.name,
          "boxId"           -> new_inkle._3.id,
          "boxSecret"       -> new_inkle._3.secret
				)

				Ok(json_inkle)
			}
		)
	}

	def extend(parentId: Long) = CanExtend(parentId) { username => implicit request =>

		val inklerId = Inkler.findIdByUsername(username).get

		inkleForm.bindFromRequest.fold(
			formWithErrors => Forbidden,
			{
				case (inkle) =>
				val id = Inkle.create(inklerId, None, Some(parentId), inkle)
				val new_inkle = Inkle.find(id)

				val json_inkle = obj(
					"id"              -> new_inkle._1.id,
					"inkle"           -> new_inkle._1.inkle,
					"createdTime"     -> new_inkle._1.created,
					"inklerId"        -> new_inkle._2.id,
					"childrenCount"   -> Inkle.childrenCount(new_inkle._1.id),
					"inklerUsername"  -> new_inkle._2.username
				)

				Ok(json_inkle)
			}
		)
	}

	def getChildren(id: Long) = Action {
		val inkles = Inkle.findChildren(id)

		val jsonInkles: JsArray = arr(
			inkles.map { inkle =>
				obj(
					"id"              -> inkle._1.id,
					"inkle"           -> inkle._1.inkle,
					"createdTime"     -> inkle._1.created,
					"inklerId"        -> inkle._2.id,
					"childrenCount"   -> Inkle.childrenCount(inkle._1.id),
					"inklerUsername"  -> inkle._2.username
				)
			}
		)

		Ok(jsonInkles)
	}

	def getInkle(id: Long) = Action {
		val inkle = Inkle.find(id)
		Ok(inkle._1.inkle)
	}

	def view(id: Long) = IsAuthenticated { username => _ =>

		Ok(html.inkle.view(Inkle.find(id), Inkle.view(id), Inkler.findByUsername(username).get))
	}


  /** ajax actions **/

  def fetchPage(page: Int) = IsAuthenticated { username => _ =>

    val inklerId = Inkler.findIdByUsername(username).get

    val inkles = Inkle.findFollowed(inklerId, page)

    val fetchedInkles: JsArray = arr(
      inkles.items.map { inkle =>
        obj(
          "id"              -> inkle._1.id,
          "inkle"           -> inkle._1.inkle,
          "parentId"        -> inkle._1.parentId,
          "createdTime"     -> inkle._1.created,
          "inklerId"        -> inkle._2.id,
          "childrenCount"   -> Inkle.childrenCount(inkle._1.id),
          "inklerUsername"  -> inkle._2.username,
          "boxOwner"        -> Inkler.findUsernameById(inkle._3.createdBy),
          "boxName"         -> inkle._3.name,
          "boxId"           -> inkle._3.id,
          "boxSecret"       -> inkle._3.secret,
          "children"        -> arr(
            Inkle.findChildren(inkle._1.id).map { child =>
              obj(
                "id"              -> child._1.id,
                "inkle"           -> child._1.inkle,
                "createdTime"     -> child._1.created,
                "inklerId"        -> child._2.id,
                "childrenCount"   -> Inkle.childrenCount(child._1.id),
                "inklerUsername"  -> child._2.username
              )
            }
          )
        )
      }
    )

    Ok(fetchedInkles)
  }

  def fetchBoxPage(box: Long, page: Int) = IsAuthenticated { username => _ =>

    val inkles = Inkle.findByBox(box, page)

    val fetchedInkles: JsArray = arr(
      inkles.items.map { inkle =>
        obj(
          "id"              -> inkle._1.id,
          "inkle"           -> inkle._1.inkle,
          "createdTime"     -> inkle._1.created,
          "inklerId"        -> inkle._2.id,
          "inklerUsername"  -> inkle._2.username,
          "boxOwner"        -> Inkler.findUsernameById(inkle._3.createdBy),
          "boxName"         -> inkle._3.name,
          "boxId"           -> inkle._3.id,
          "boxSecret"       -> inkle._3.secret,
          "children"        -> arr(
            Inkle.findChildren(inkle._1.id).map { child =>
              obj(
                "id"              -> child._1.id,
                "inkle"           -> child._1.inkle,
                "createdTime"     -> child._1.created,
                "inklerId"        -> child._2.id,
                "inklerUsername"  -> child._2.username
              )
            }
          )
        )
      }
    )

    Ok(fetchedInkles)
  }

  def fetchOopsPage(page: Int) = Action {

    val inkles = Inkle.findOops(page)

    val fetchedInkles: JsArray = arr(
      inkles.items.map { inkle =>
        obj(
          "id"              -> inkle._1.id,
          "inkle"           -> inkle._1.inkle,
          "createdTime"     -> inkle._1.created,
          "inklerId"        -> inkle._2.id,
          "childrenCount"   -> Inkle.childrenCount(inkle._1.id),
          "inklerUsername"  -> inkle._2.username,
          "boxOwner"        -> Inkler.findUsernameById(inkle._3.createdBy),
          "boxName"         -> inkle._3.name,
          "boxId"           -> inkle._3.id,
          "children"        -> arr(
            Inkle.findChildren(inkle._1.id).map { child =>
              obj(
                "id"              -> child._1.id,
                "inkle"           -> child._1.inkle,
                "createdTime"     -> child._1.created,
                "inklerId"        -> child._2.id,
                "childrenCount"   -> Inkle.childrenCount(child._1.id),
                "inklerUsername"  -> child._2.username
              )
            }
          )
        )
      }
    )

    Ok(fetchedInkles)
  }

  def getParent(id: Long) = Action {

    val inkle = Inkle.find(id)

    val fetchedInkles: JsObject = obj(
      "id"              -> inkle._1.id,
      "inkle"           -> inkle._1.inkle,
      "parentId"        -> inkle._1.parentId,
      "createdTime"     -> inkle._1.created,
      "inklerId"        -> inkle._2.id,
      "childrenCount"   -> Inkle.childrenCount(inkle._1.id),
      "inklerUsername"  -> inkle._2.username
    )

    Ok(fetchedInkles)

  }
}
