package gate

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import com.github.jasync.sql.db.SuspendingConnection
import common.CommonCommandDao
import common.CommonDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.joda.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import statistics.StatsState

class DaoTest {
    private val TIMESTAMP_13 = "1862-04-13T20:00:00"
    private val TIMESTAMP_15 = "1862-04-15T20:00:00"
    @Test
    fun<T> enterTest() = runBlocking {
        val enterTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.getUserQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.maxSubscriptionDateQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    val rowData = mockk<RowData>()
                    every { rowData.getAs<LocalDateTime>("end_date") }.returns(
                        LocalDateTime.parse(TIMESTAMP_15)
                    )
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.lastGateEventQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(0)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonCommandDao.maxUserEventIdQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    val rowData = mockk<RowData>()
                    every { rowData.getInt("max") }.returns(14)
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.newEventCommand, listOf(1, 15))
                }.answers {
                    QueryResult(rowsAffected = 1, statusMessage = "OK")
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(
                        GymCommandDaoImpl.newGateEventCommand,
                        listOf(1, 15, GateEventType.ENTER, enterTime)
                    )
                }.answers {
                    QueryResult(rowsAffected = 1, statusMessage = "OK")
                }

                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)

        val result = dao.processGymComand(GymCommand.EnterGymCommand(1, enterTime))
        assertEquals(result, Unit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun<T> doubleEnterTest() = runBlocking {
        val enterTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.getUserQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.maxSubscriptionDateQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    val rowData = mockk<RowData>()
                    every { rowData.getAs<LocalDateTime>("end_date") }.returns(
                        LocalDateTime.parse(TIMESTAMP_15)
                    )
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.lastGateEventQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    val rowData = mockk<RowData>()
                    every { rowData.getString("gate_event_type") }.returns("ENTER")
                    every { rowData.getAs<LocalDateTime>("event_timestamp") }.returns(
                        LocalDateTime.parse(TIMESTAMP_13)
                    )
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)
        dao.processGymComand(GymCommand.EnterGymCommand(1, enterTime))
    }

    @Test
    fun<T> exitTest() = runBlocking {
        val exitTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val enterTime = LocalDateTime.parse(TIMESTAMP_13)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.lastGateEventQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    val rowData = mockk<RowData>()
                    every { rowData.getString("gate_event_type") }.returns("ENTER")
                    every { rowData.getAs<LocalDateTime>("event_timestamp") }.returns(enterTime)
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonCommandDao.maxUserEventIdQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    val rowData = mockk<RowData>()
                    every { rowData.getInt("max") }.returns(14)
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.newEventCommand, listOf(1, 15))
                }.answers {
                    QueryResult(rowsAffected = 1, statusMessage = "OK")
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(
                        GymCommandDaoImpl.newGateEventCommand,
                        listOf(1, 15, GateEventType.EXIT, exitTime)
                    )
                }.answers {
                    QueryResult(rowsAffected = 1, statusMessage = "OK")
                }

                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)
        val result = dao.processGymComand(GymCommand.ExitGymCommand(1, exitTime))
        assertEquals(result, enterTime)
    }

    @Test(expected = IllegalArgumentException::class)
    fun<T> doubleExitTest() = runBlocking {
        val exitTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val prevExitTime = LocalDateTime.parse(TIMESTAMP_13)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.lastGateEventQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    val rowData = mockk<RowData>()
                    every { rowData.getString("gate_event_type") }.returns("EXIT")
                    every { rowData.getAs<LocalDateTime>("event_timestamp") }.returns(prevExitTime)
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }
                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)
        dao.processGymComand(GymCommand.ExitGymCommand(1, exitTime))
        Unit
    }

    @Test(expected = IllegalArgumentException::class)
    fun<T> unexpectedExitTest() = runBlocking {
        val exitTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.lastGateEventQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(0)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }
                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)
        dao.processGymComand(GymCommand.ExitGymCommand(1, exitTime))
        Unit
    }

    @Test
    fun<T> enterExitEnterTest() = runBlocking {
        val enterTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.getUserQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.maxSubscriptionDateQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    val rowData = mockk<RowData>()
                    every { rowData.getAs<LocalDateTime>("end_date") }.returns(
                        LocalDateTime.parse(TIMESTAMP_15)
                    )
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.lastGateEventQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    val rowData = mockk<RowData>()
                    every { rowData.getString("gate_event_type") }.returns("EXIT")
                    every { rowData.getAs<LocalDateTime>("event_timestamp") }.returns(
                        LocalDateTime.parse(TIMESTAMP_13)
                    )
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonCommandDao.maxUserEventIdQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    val rowData = mockk<RowData>()
                    every { rowData.getInt("max") }.returns(14)
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.newEventCommand, listOf(1, 15))
                }.answers {
                    QueryResult(rowsAffected = 1, statusMessage = "OK")
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(
                        GymCommandDaoImpl.newGateEventCommand,
                        listOf(1, 15, GateEventType.ENTER, enterTime)
                    )
                }.answers {
                    QueryResult(rowsAffected = 1, statusMessage = "OK")
                }

                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)

        val result = dao.processGymComand(GymCommand.EnterGymCommand(1, enterTime))
        assertEquals(result, Unit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun<T> enterNoExitTest() = runBlocking {
        val enterTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.getUserQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(0)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }
                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)
        dao.processGymComand(GymCommand.EnterGymCommand(1, enterTime))
    }

    @Test(expected = IllegalArgumentException::class)
    fun<T> unregisteredEnterTest() = runBlocking {
        val enterTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.getUserQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.maxSubscriptionDateQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(0)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(GymCommandDaoImpl.lastGateEventQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(0)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)
        dao.processGymComand(GymCommand.EnterGymCommand(1, enterTime))
    }

    @Test(expected = IllegalArgumentException::class)
    fun<T> badSubscriptionTest() = runBlocking {
        val enterTime = LocalDateTime.parse(StatsState.INITIAL_TIMESTAMP)
        val mainConnection = mockk<SuspendingConnection>()
        coEvery { mainConnection.inTransaction(any<suspend (SuspendingConnection) -> T>()) }
            .coAnswers {
                val callback = args[0] as suspend (SuspendingConnection) -> T
                val transactionConnection = mockk<SuspendingConnection>()

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.getUserQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }

                coEvery {
                    transactionConnection.sendPreparedStatement(CommonDao.maxSubscriptionDateQuery, listOf(1))
                }.answers {
                    val rows = mockk<ResultSet>()
                    every { rows.size }.returns(1)
                    val rowData = mockk<RowData>()
                    every { rowData.getAs<LocalDateTime>("end_date") }.returns(
                        LocalDateTime.parse(TIMESTAMP_13)
                    )
                    every { rows[0] }.returns(rowData)
                    QueryResult(rowsAffected = 0, statusMessage = "OK", rows = rows)
                }
                callback(transactionConnection)
            }
        val dao = GymCommandDaoImpl(mainConnection)
        dao.processGymComand(GymCommand.EnterGymCommand(1, enterTime))
    }
}
