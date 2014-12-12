
import org.anormcypher.Neo4jREST
import play.api._
import play.api.http.HeaderNames._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

import play.Play._

import tools.Loggers._

object Global extends GlobalSettings {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		globalLogger(log, params)
	}

	override def onStart(app: Application) = {
		log("onStart", Map("app" -> app))

		if (isProd) {
			Neo4jREST.setServer(
				"inklin.sb02.stations.graphenedb.com",
				24789,
				"/db/data/",
				"inklin",
				"ycazBrL2WBSLiggnTSMR",
				"ext/CypherPlugin/graphdb/execute_query"
			)
		} else {
			Neo4jREST.setServer(
				"localhost",
				7474,
				"/db/data/",
				"",
				"",
				"ext/CypherPlugin/graphdb/execute_query"
			)
		}
	}

	override def onRequestCompletion(request: RequestHeader) {
		log("onRequestCompletion: [" + request.method + "] " + request.path)
	}

	override def onHandlerNotFound(request: RequestHeader): Future[play.api.mvc.Result] = {
		log("onHandlerNotFound: [" + request.method + "] " + request.path)

		tools.Email.send(
			">> HANDLER NOT FOUND",
			s"""
			  |<b>session email:</b> ${request.session.get("email").getOrElse("NONE")} <br>
			  |<br>
			  |<b>request method:</b> ${request.method} <br>
			  |<b>request domain:</b> ${request.domain} <br>
			  |<b>request path:</b> ${request.path} <br>
			  |<b>referer:</b> ${request.headers.get(REFERER)} <br>
			  |<b>user agent:</b> ${request.headers.get(USER_AGENT)} <br>
			  |<b>flash:</b> ${request.flash.toString} <br>
			  |<b>request raw query string:</b> ${request.rawQueryString} <br>
			""".stripMargin,
			"ameernuri@gmail.com"
		)
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
