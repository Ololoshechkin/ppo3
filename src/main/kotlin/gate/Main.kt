package gate

import com.typesafe.config.ConfigFactory
import common.ConnectionPoolProvider
import common.getUid
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.joda.time.LocalDateTime
import java.nio.file.Paths

fun main(): Unit = runBlocking {
    val pathToConfig = Paths.get("src/main/resources/gate.conf")
    val config = ConfigFactory.parseFile(pathToConfig.toFile())
    val applicationConfig = GateApplicationConfig(config)
    val connection = ConnectionPoolProvider.getConnection(applicationConfig.databaseConfig)
    val dao = GymCommandDaoImpl(connection)
    val clientsProvider = StatsHttpClientsProviderImpl(applicationConfig.statsConfig)
    val commandProcessor = CommandSuspendProcessor(dao, clientsProvider)

    embeddedServer(Netty, port = applicationConfig.apiConfig.port) {
        routing {
            get("/command/enter") {
                val uid = call.request.queryParameters.getUid()
                val enterTime = LocalDateTime.now()
                val command = GymCommand.EnterGymCommand(uid, enterTime)
                call.respondText(commandProcessor.processOrError(command))
            }
            get("/command/exit") {
                val uid = call.request.queryParameters.getUid()
                val exitTime = LocalDateTime.now()
                val command = GymCommand.ExitGymCommand(uid, exitTime)
                call.respondText(commandProcessor.processOrError(command))
            }
        }
    }.start(wait = true)
    Unit
}
