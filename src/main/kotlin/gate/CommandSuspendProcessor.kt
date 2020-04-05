package gate

import common.SuspendProcessor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory

class CommandSuspendProcessor(
    private val gymCommandDao: GymCommandDao,
    private val statsHttpClientsProvider: StatsHttpClientsProvider
) : SuspendProcessor<GymCommand> {

    override suspend fun process(cmd: GymCommand) = when (cmd) {
        is GymCommand.EnterGymCommand -> {
            gymCommandDao.processGymComand(GymCommand.EnterGymCommand(cmd.userId, cmd.timestamp))
            "User(id=${cmd.userId}) entered"
        }
        is GymCommand.ExitGymCommand -> {
            val enterTimestamp = gymCommandDao.processGymComand(
                GymCommand.ExitGymCommand(cmd.userId, cmd.timestamp)
            ) as LocalDateTime
            val client = statsHttpClientsProvider.getClient()

            GlobalScope.launch {
                try {
                    client.exitCommand(cmd.userId, enterTimestamp, cmd.timestamp)
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }
            }
            "User(id=${cmd.userId}) exited"
        }
    }
}
