
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

import tools.Loggers._

import org.apache.commons.lang3.RandomStringUtils._

object Global extends GlobalSettings {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		globalLogger(log, params)
	}

	override def onStart(app: Application) = {
		log("onStart", Map("app" -> app))
	}

	override def onRequestCompletion(request: RequestHeader) {
		log("onRequestCompletion: [" + request.method + "] " + request.path)
	}

	override def onHandlerNotFound(request: RequestHeader): Future[play.api.mvc.SimpleResult] = {
		log("onHandlerNotFound: [" + request.method + "] " + request.path)

		Future.successful(
			NotFound("not found")
		)
	}

	override def onError(request: RequestHeader, ex: Throwable) = {
		log("onError: [" + request.method + "] " + request.path)

		Future.successful(
			InternalServerError("internal server error")
		)
	}

	override def onBadRequest(request: RequestHeader, error: String) = {
		log("onBadRequest: [" + request.method + "] " + request.path)

		Future.successful(
			BadRequest("bad request")
		)
	}
}
