package monkeys

import monkeys.Loggers._

object CronJobs {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		toolsLogger("CronJobs", log, params)
	}
}
