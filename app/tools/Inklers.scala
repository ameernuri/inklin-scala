package tools

import tools.Loggers._

object Inklers {

	private def log(log: String, params: Map[String, Any] = Map()) = toolsLogger("People", log, params)

	/**
	 * get the inklin address of the inkler
	 *
	 * @param id id of the inkler
	 * @return
	 */
	def getAddress(id: Long): String = {
    log("getAddress", Map("id" -> id))

		""
	}

	/**
	 * get the inklin address of the inkler
	 *
	 * @param inkler the inkler
	 * @return
	 */
	def getAddress(inkler: models.Inkler): String = {
    log("getAddress", Map("inkler" -> inkler.id))

		""
	}
}
