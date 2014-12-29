package monkeys

object Loggers {

	private def consoleColor(color: String, content: String): String = {
		val ink = color.toLowerCase match {
			case "green" => Console.GREEN
			case "cyan" => Console.CYAN
			case "blue" => Console.BLUE
			case _ => Console.WHITE
		}

		ink + content + Console.RESET
	}

	private def consoleBold(content: String): String = {
		Console.BOLD + content + Console.RESET
	}

	/**
	 * model logger
	 *
	 * @param model model name
	 * @param log the method log
	 * @param params map of params
	 */
	def modelLogger(
		model: String,
		log: String,
		params: Map[String, Any] = Map()
	) {
		var print = "Model ### " + model + " > " + consoleBold(log)

		params.map { a =>
			print += " | " + consoleColor("cyan", a._1 + ": " +  a._2)
		}

		println(print)
	}

	/**
	 * controller logger
	 *
	 * @param controller controller name
	 * @param log the method log
	 * @param params map of params
	 */
	def controllerLogger(
		controller: String,
		log: String,
		params: Map[String, Any] = Map()
	) {
		var print = consoleColor("blue", consoleBold("Ctrlr >>> ")) +
		controller + " > " +
		log

		params.map { a =>
			print += " | " + consoleColor("cyan", a._1 + ": " +  a._2)
		}

		println()
		println(print)
		println()
	}

	/**
	 * tools logger
	 *
	 * @param tool the tool name
	 * @param log the method log
	 * @param params map of params
	 */
	def toolsLogger(
		tool: String,
		log: String,
		params: Map[String, Any] = Map()
	) = {
		var print = "Tools ... " + tool + " > " + consoleBold(log)

		params.map { a =>
			print += " | " + consoleColor("cyan", a._1 + ": " +  a._2)
		}

		println()
		println(print)
	}

	/**
	 * guard logger
	 *
	 * @param log the guard name
	 * @param params map of params
	 */
	def guardLogger(
		log: String,
		params: Map[String, Any] = Map()
	) = {
		var print = "Guard ... " + consoleBold(log)

		params.map { a =>
			print += " | " + consoleColor("cyan", a._1 + ": " +  a._2)
		}

		println()
		println(print)
	}

	/**
	 * global logger
	 *
	 * @param log the method log
	 * @param params map of params
	 */
	def globalLogger(
		log: String,
		params: Map[String, Any] = Map()
	) = {
		var print = "Globl ... " + log

		params.map { a =>
			print += " | " + consoleColor("cyan", a._1 + ": " +  a._2)
		}

		println(print)
	}
}
