package common

import io.ktor.http.Parameters
import org.joda.time.LocalDateTime

fun Parameters.getUid(): Int {
    return this["userId"]?.toInt() ?: -1
}

fun Parameters.getTimestamp(paramName: String): LocalDateTime {
    return LocalDateTime.parse(this[paramName] ?: "1862-04-14T00:00:00")
}

fun LocalDateTime.serialize(): String = this.toString("yyyy-MM-dd'T'HH:mm:ss")