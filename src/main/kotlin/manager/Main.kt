package manager

import common.RealTimeClock
import common.ConnectionPoolProvider
import common.getTimestamp
import common.getUid
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun main(): Unit = runBlocking {
    val config = ManagerConfig(ConfigFactory.parseFile(Paths.get("src/main/resources/manager.conf").toFile()))

    val connection = ConnectionPoolProvider.getConnection(config.databaseConfig)
    val commandDao = CommandDaoImpl(connection, RealTimeClock)

    val queryDao = QueryDaoImpl(connection)

    val commandProcessor = CommandSuspendProcessor(commandDao)
    val queryProcessor = QuerySuspendProcessor(queryDao)

    embeddedServer(Netty, port = config.apiConfig.port) {
        routing {
            get("/command/new_uid") {
                call.respondText(commandProcessor.processOrError(NewUserCommand))
            }
            get("/command/renewal") {
                val uid = call.request.queryParameters.getUid()
                val until = call.request.queryParameters.getTimestamp("until")
                val command = SubscriptionRenewalCommand(uid, until)
                call.respondText(commandProcessor.processOrError(command))
            }
            get("query/get_user") {
                val uid = call.request.queryParameters.getUid()
                val query = GetUserQuery(uid)
                call.respondText(queryProcessor.processOrError(query))
            }
        }
    }.start(wait = true)
    Unit
}
