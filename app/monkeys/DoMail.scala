package monkeys

import com.typesafe.plugin._
import org.apache.commons.mail._
import play.api.Play.current
import play.twirl.api.Html
import monkeys.DoLog._
import play.libs.Akka
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object DoMail {

	private def log(log: String, params: Map[String, Any] = Map()) = toolsLogger("DoMail", log, params)

	def sendHtml(subject: String, message: Html, to: String, fromEmail: String = "hello@inklin.co", fromName: String = "Inklin"): Unit = {
		log("sendHtml", Map(
			"subject" -> subject,
			"message" -> message,
			"to" -> to,
			"fromEmail" -> fromEmail,
			"fromName" -> fromName
		))

		val account = new DefaultAuthenticator(fromEmail, "s]?e*UT-9vB]SNz")
		val mail = new HtmlEmail()

		mail.setHostName("smtp.zoho.com")
		mail.setSmtpPort(465)
		mail.setAuthenticator(account)
		mail.setSSLOnConnect(true)
		mail.setFrom(s"$fromName <$fromEmail>")
		mail.setSubject(subject)
		mail.setHtmlMsg(message.toString())
		mail.addTo(to)

		Akka.system.scheduler.scheduleOnce(0.seconds) {

			println("sending...")
			mail.send()
			println("sent to " + to)
		}
	}
}
