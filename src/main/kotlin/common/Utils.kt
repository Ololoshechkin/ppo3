package common

import io.ktor.http.Parameters
import org.joda.time.LocalDateTime
import statistics.StatsState

fun Parameters.getUid(): Int {
    return this["userId"]?.toInt() ?: -1
}

fun Parameters.getTimestamp(paramName: String): LocalDateTime {
    return LocalDateTime.parse(this[paramName] ?: StatsState.INITIAL_TIMESTAMP)
}

fun LocalDateTime.tostr(): String = this.toString("yyyy-MM-dd'T'HH:mm:ss")