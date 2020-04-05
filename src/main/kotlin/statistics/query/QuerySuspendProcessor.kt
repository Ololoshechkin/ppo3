package statistics.query

import common.SuspendProcessor

class QuerySuspendProcessor(private val stats: QueryableStats) : SuspendProcessor<Query> {
    override suspend fun process(cmd: Query): String {
        return when (cmd) {
            is GetUserStatsQuery -> {
                stats.getUserStats(cmd.uid)?.let {
                    val normalizedPeriod = it.totalTime.normalizedStandard()
                    "${normalizedPeriod.years} years, " +
                            "${normalizedPeriod.months} months, " +
                            "${normalizedPeriod.weeks} weeks, " +
                            "${normalizedPeriod.days} days, " +
                            "${normalizedPeriod.hours} hours, " +
                            "${normalizedPeriod.minutes} minutes, " +
                            "${normalizedPeriod.seconds} seconds, " +
                            "${normalizedPeriod.millis} milliseconds spent; " +
                            "${it.visitsCount} total visits"
                } ?: "No such user"
            }
        }
    }

}
