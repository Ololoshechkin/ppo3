package statistics

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import com.github.jasync.sql.db.SuspendingConnection
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.joda.time.LocalDateTime
import org.joda.time.Period
import org.junit.Test
import org.junit.Assert.*

class StatisticsTest {
    @Test
    fun singleStateUpdateTest() = runBlocking {
        val mainConnection = mockk<SuspendingConnection>()
        coEvery {
            mainConnection.sendQuery(StatsState.getStatsQuery)
        }.answers {
            val rows = mockk<ResultSet>()
            val iterator = mockk<Iterator<RowData>>()
            every { iterator.hasNext() }.returns(false)
            every { rows.iterator() }.returns(iterator)
            QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
        }
        val stats = StatsState()
        stats.init(mainConnection)
        val exitTime = LocalDateTime.now()
        val enterTime = exitTime.minusHours(1)
        stats.updateState(1, enterTime, exitTime)
        assertEquals(stats.getUserStats(1), UserStats(Period.hours(1), 1))
    }

    @Test
    fun multipleStateUpdatesTest() = runBlocking {
        val mainConnection = mockk<SuspendingConnection>()
        coEvery {
            mainConnection.sendQuery(StatsState.getStatsQuery)
        }.answers {
            val rows = mockk<ResultSet>()
            val iterator = mockk<Iterator<RowData>>()
            every { iterator.hasNext() }.returns(false)
            every { rows.iterator() }.returns(iterator)
            QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
        }
        val stats = StatsState()
        stats.init(mainConnection)

        val exitTime = LocalDateTime.now()
        val enterTime = exitTime.minusHours(1)
        stats.updateState(1, enterTime, exitTime)
        val anotherExitTime = LocalDateTime.now().plus(Period.hours(3))
        val anotherEnterTime = anotherExitTime.minusHours(2)
        stats.updateState(1, anotherEnterTime, anotherExitTime)
        assertEquals(stats.getUserStats(1), UserStats(Period.hours(3), 2))
        stats.updateState(2, enterTime, exitTime)
        assertEquals(stats.getUserStats(2), UserStats(Period.hours(1), 1))
        assertEquals(stats.getUserStats(3), null)
    }
}
