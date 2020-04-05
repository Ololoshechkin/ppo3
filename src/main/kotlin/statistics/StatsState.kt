package statistics

import com.github.jasync.sql.db.SuspendingConnection
import statistics.command.UpdatableStats
import statistics.model.UserStats
import statistics.query.QueryableStats
import org.joda.time.LocalDateTime
import org.joda.time.Period
import java.util.concurrent.ConcurrentHashMap

class StatsState : UpdatableStats, QueryableStats {
    private val state = ConcurrentHashMap<Int, UserStats>()

    /*
    Should be called once from a single thread.
    Call of init should happen-before call of updateState
    Reference should be correctly published
     */
    suspend fun init(connection: SuspendingConnection) {
        val result = connection.sendQuery(getStatsQuery).rows
        for (curRow in result) {
            val uid = curRow.getInt("user_id")!!
            val interval = curRow.getAs<Period>("total_interval")
            val visitsCount = curRow.getLong("visits_count")!!.toInt()
            state[uid] = UserStats(interval, visitsCount)
        }
    }

    // Thread-safe
    override fun updateState(uid: Int, enterTime: LocalDateTime, exitTime: LocalDateTime) {
        val period = Period.fieldDifference(enterTime, exitTime)
        state.compute(uid) { _, curStats ->
            if (curStats == null) {
                UserStats(period.normalizedStandard(), 1)
            } else {
                UserStats(curStats.totalTime.plus(period).normalizedStandard(), curStats.visitsCount + 1)
            }
        }
    }

    override fun getUserStats(uid: Int): UserStats? = state[uid]

    companion object {
        const val INITIAL_TIMESTAMP = "1862-04-14T00:00:00"
        val getStatsQuery =
            """
                with UserExitTotalTimestamp as (
                    select GateIOEvents.user_id,
                           sum(GateIOEvents.event_timestamp - '$INITIAL_TIMESTAMP' :: timestamp) as exit_sum
                    from GateIOEvents
                    where GateIOEvents.gate_event_type = 'EXIT'
                    group by GateIOEvents.user_id
                ),
                     UserEnterTotalTimestamp as (
                         select GateIOEvents.user_id,
                                sum(GateIOEvents.event_timestamp - '$INITIAL_TIMESTAMP' :: timestamp) AS enter_sum
                         from GateIOEvents
                         where GateIOEvents.gate_event_type = 'ENTER'
                         group BY GateIOEvents.user_id
                     ),
                     UserLastEvent as (
                         select GateIOEvents.user_id,
                                max(GateIOEvents.user_event_id) as user_event_id
                         from GateIOEvents
                         group by GateIOEvents.user_id
                     ),
                     LastEventByUser as (
                         select GateIOEvents.user_id,
                                GateIOEvents.user_event_id,
                                GateIOEvents.event_timestamp,
                                GateIOEvents.gate_event_type
                         from GateIOEvents
                                  natural join UserLastEvent
                     ),
                     SubtractByUser as (
                         select (LastEventByUser.event_timestamp - '$INITIAL_TIMESTAMP') *
                                (case when LastEventByUser.gate_event_type = 'EXIT' then 0 else 1 end)
                                    as subtraction
                         from LastEventByUser
                     ),
                     UserExitEvents as (
                         select GateIOEvents.user_id,
                                GateIOEvents.user_event_id
                         from GateIOEvents
                         where GateIOEvents.gate_event_type = 'EXIT'
                     ),
                     UserExitTotalTimestamp as (
                         select UserExitEvents.user_id,
                                count(*) AS exits_count
                         from UserExitEvents
                         group by UserExitEvents.user_id
                     )
                select UserExitTotalTimestamp.user_id,
                       UserExitTotalTimestamp.exit_sum - (UserEnterTotalTimestamp.enter_sum - SubtractByUser.subtraction) as total_interval,
                       UserExitTotalTimestamp.exits_count as visits_count
                from UserExitTotalTimestamp
                         natural join UserEnterTotalTimestamp
                         natural join SubtractByUser
                         natural join UserExitTotalTimestamp;
            """.trimIndent()
    }
}
