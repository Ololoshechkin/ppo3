package statistics

import common.SuspendProcessor
import org.joda.time.LocalDateTime

data class RegisterExitCommand(
    val userId: Int,
    val enterTimestamp: LocalDateTime, val exitTimestamp: LocalDateTime
)

interface UpdatableStats {
    fun updateState(userId: Int, enterTime: LocalDateTime, exitTime: LocalDateTime)
}

class CommandSuspendProcessor(private val stats: UpdatableStats) : SuspendProcessor<RegisterExitCommand> {
    override suspend fun process(cmd: RegisterExitCommand) =
        stats.updateState(cmd.userId, cmd.enterTimestamp, cmd.exitTimestamp).let { "OK" }
}
