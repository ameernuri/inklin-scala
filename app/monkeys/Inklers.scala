package monkeys

import monkeys.Loggers._

object Inklers {

	private def log(log: String, params: Map[String, Any] = Map()) = toolsLogger("People", log, params)

	/**
	 * get the inklin address of the inkler
	 *
	 * @param uuid uuid of the inkler
	 * @return
	 */
	def getAddress(uuid: String): String = {
    log("getAddress", Map("uuid" -> uuid))

		""
	}

	/**
	 * get the inklin address of the inkler
	 *
	 * @param inkler the inkler
	 * @return
	 */
	def getAddress(inkler: models.Inkler): String = {
    log("getAddress", Map("inkler" -> inkler.uuid))

		""
	}
}
