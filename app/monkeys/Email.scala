package monkeys

import com.typesafe.plugin._
import play.api.Play.current
import play.api.templates.Html
import monkeys.Loggers._
import play.libs.Akka
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Email {

	private def log(log: String, params: Map[String, Any] = Map()) = toolsLogger("Email", log, params)

	/**
	 * send email
	 *
	 * @param subject subject of the email
	 * @param message message of the email
	 * @param to receiver
	 * @param from sender name
	 */
	def send(subject: String, message: String, to: String, from: String = "Persona") {
		log("send", Map("subject" -> subject, "message" -> message, "to" -> to, "from" -> from))

		val mail = use[MailerPlugin].email

		val renderedMessage =
			"<html>" +
			"<a href='http://inklin.co'>" +
			"<img src='http://inklin.co/assets/images/logo.png' alt='Persona'>" +
			"</a>" +
			message +
			"</html>"

		Akka.system.scheduler.scheduleOnce(0 seconds) {
			mail.setSubject(subject)
			mail.setRecipient(to)
			mail.setFrom(from + " <app@inklin.co>")

			mail.sendHtml(renderedMessage)
			println("sent")
		}
	}

	/**
	 * send html email
	 *
	 * @param subject subject of the email
	 * @param html the html to send
	 * @param to receiver's email
	 * @param from sender's name
	 */
	def sendHtml(subject: String, html: Html, to: String, from: String = "Persona") {
		log("sendHtml", Map("subject" -> subject, "html" -> html, "to" -> to, "from" -> from))

		Akka.system.scheduler.scheduleOnce(0 seconds) {
			val mail = use[MailerPlugin].email
			mail.setSubject(subject)
			mail.setRecipient(to)
			mail.setFrom(from + " <app@inklin.co>")
			mail.sendHtml(html.toString())
		}
	}
}
