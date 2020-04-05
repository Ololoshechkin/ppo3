package statistics

import common.ConnectionPoolProvider
import common.getTimestamp
import common.getUid
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.nio.file.Paths

fun main(): Unit = runBlocking {
    val config =
        StatisticsConfig(ConfigFactory.parseFile(Paths.get("src/main/resources/statistics.conf").toFile()))
    val stateHolder = StatsState()

    val queryProcessor = StatisticsQuerySuspendProcessor(stateHolder)
    val commandProcessor = CommandSuspendProcessor(stateHolder)

    stateHolder.init(connection = ConnectionPoolProvider.getConnection(config.databaseConfig))
    embeddedServer(Netty, port = config.apiConfig.port) {
        routing {
            get("/query/get_stats") {
                val uid = call.request.queryParameters.getUid()
                val query = GetUserStatsQuery(uid)
                call.respondText(queryProcessor.processOrError(query))
            }
            get("command/exit") {
                val uid = call.request.queryParameters.getUid()
                val enterTime = call.request.queryParameters.getTimestamp("enter")
                val exitTime = call.request.queryParameters.getTimestamp("exit")
                val command = RegisterExitCommand(uid, enterTime, exitTime)
                call.respondText(commandProcessor.processOrError(command))
            }
        }
    }.start(wait = true)
    Unit
}
