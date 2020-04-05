package statistics.command

import common.SuspendProcessor

class CommandSuspendProcessor(private val stats: UpdatableStats) : SuspendProcessor<Command> {
    override suspend fun process(cmd: Command): String {
        return when (cmd) {
            is RegisterExitCommand -> {
                stats.updateState(cmd.uid, cmd.enterTimestamp, cmd.exitTimestamp)
                "OK"
            }
        }
    }
}
