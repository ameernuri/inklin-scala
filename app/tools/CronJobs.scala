package tools

import tools.Loggers._

object CronJobs {

	private def log(log: String, params: Map[String, Any] = Map()) = {
		toolsLogger("CronJobs", log, params)
	}
}
