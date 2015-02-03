package monkeys

import monkeys.DoLog._

object Users {

	private def log(log: String, params: Map[String, Any] = Map()) = toolsLogger("People", log, params)

	/**
	 * get the inklin address of the user
	 *
	 * @param uuid uuid of the user
	 * @return
	 */
	def getAddress(uuid: String): String = {
    log("getAddress", Map("uuid" -> uuid))

		""
	}

	/**
	 * get the inklin address of the user
	 *
	 * @param user the user
	 * @return
	 */
	def getAddress(user: models.User): String = {
    log("getAddress", Map("user" -> user.uuid))

		""
	}
}
