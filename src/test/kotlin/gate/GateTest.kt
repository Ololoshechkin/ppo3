package gate

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.joda.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GateTest {
    @Test
    fun correctExitTest() = runBlocking {
        val exitTime = LocalDateTime.parse("1998-04-14T20:00:00")
        val enterTime = LocalDateTime.parse("1998-04-13T20:00:00")
        val commandDao = mockk<GymCommandDao>()
        coEvery {
            commandDao.processGymComand(GymCommand.ExitGymCommand(1, exitTime))
        }.returns(enterTime)

        val countDownLatch = CountDownLatch(1)
        val statsAccessCounter = AtomicInteger(0)
        val clientsProvider = mockk<StatsHttpClientsProvider>()
        every {
            clientsProvider.getClient()
        }.answers {
            val client = mockk<StatsHttpClient>()
            coEvery {
                client.exitCommand(1, enterTime, exitTime)
            }.answers {
                statsAccessCounter.incrementAndGet()
                countDownLatch.countDown()
                "OK"
            }
            client
        }
        val commandProcessor = CommandSuspendProcessor(commandDao, clientsProvider)
        commandProcessor.processOrError(GymCommand.ExitGymCommand(1, exitTime))
        countDownLatch.await(50, TimeUnit.MILLISECONDS)
        assertEquals(statsAccessCounter.get(), 1)
    }

    @Test
    fun statsUnavailableTest() = runBlocking {
        val exitTime = LocalDateTime.parse("1998-04-14T20:00:00")
        val enterTime = LocalDateTime.parse("1998-04-13T20:00:00")
        val commandDao = mockk<GymCommandDao>()
        coEvery {
            commandDao.processGymComand(GymCommand.ExitGymCommand(1, exitTime))
        }.returns(enterTime)

        val countDownLatch = CountDownLatch(1)
        val statsAccessCounter = AtomicInteger(0)
        val clientsProvider = mockk<StatsHttpClientsProvider>()
        every {
            clientsProvider.getClient()
        }.answers {
            val client = mockk<StatsHttpClient>()
            coEvery {
                client.exitCommand(1, enterTime, exitTime)
            }.answers {
                countDownLatch.countDown()
                throw Exception("Connection refused")
            }
            client
        }
        val commandProcessor = CommandSuspendProcessor(commandDao, clientsProvider)
        commandProcessor.processOrError(GymCommand.ExitGymCommand(1, exitTime))
        countDownLatch.await(50, TimeUnit.MILLISECONDS)
        assertEquals(statsAccessCounter.get(), 0)
    }

    @Test
    fun incorrectExit() = runBlocking {
        val exitTime = LocalDateTime.parse("1998-04-14T20:00:00")
        val commandDao = mockk<GymCommandDao>()
        coEvery {
            commandDao.processGymComand(GymCommand.ExitGymCommand(1, exitTime))
        }.throws(Exception("Error exiting"))

        val res = CommandSuspendProcessor(
            gymCommandDao = commandDao,
            statsHttpClientsProvider = mockk()
        ).processOrError(GymCommand.ExitGymCommand(1, exitTime))
        assertTrue(res.startsWith("Error occurred when processing"))
    }
}
