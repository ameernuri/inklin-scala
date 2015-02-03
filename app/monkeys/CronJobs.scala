package monkeys

import monkeys.DoLog._

object CronJobs {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		toolsLogger("CronJobs", log, params)
	}
}
