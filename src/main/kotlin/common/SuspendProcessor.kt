package common

interface SuspendProcessor<CMD> {
    suspend fun processOrError(cmd: CMD): String = try {
        process(cmd)
    } catch (e: Exception) {
        "Error occurred when processing $cmd: ${e.message}"
    }

    suspend fun process(cmd: CMD): String
}
