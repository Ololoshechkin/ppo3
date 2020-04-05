package statistics

import common.SuspendProcessor
import org.joda.time.Period

data class UserStats(val totalTime: Period, val visitsCount: Int)

data class GetUserStatsQuery(val userId: Int)

interface QueryableStats {
    fun getUserStats(userId: Int): UserStats?
}

class StatisticsQuerySuspendProcessor(private val stats: QueryableStats) : SuspendProcessor<GetUserStatsQuery> {
    override suspend fun process(cmd: GetUserStatsQuery) =
        stats.getUserStats(cmd.userId)?.totalTime?.normalizedStandard()?.toString()
            ?: "User not found"

}