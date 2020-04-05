package statistics

import common.ConnectionPoolProvider
import common.getTimestamp
import common.getUid
import statistics.command.CommandSuspendProcessor
import statistics.command.RegisterExitCommand
import statistics.config.ApplicationConfigImpl
import statistics.query.GetUserStatsQuery
import statistics.query.QuerySuspendProcessor
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.nio.file.Paths

fun main(): Unit = runBlocking {
    val pathToConfig = Paths.get("src/main/resources/statistics.conf")
    val config = ConfigFactory.parseFile(pathToConfig.toFile())
    val applicationConfig = ApplicationConfigImpl(config)
    val connection = ConnectionPoolProvider.getConnection(applicationConfig.databaseConfig)
    val stateHolder = StatsState()
    val queryProcessor = QuerySuspendProcessor(stateHolder)
    val commandProcessor = CommandSuspendProcessor(stateHolder)

    stateHolder.init(connection)
    embeddedServer(Netty, port = applicationConfig.apiConfig.port) {
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
