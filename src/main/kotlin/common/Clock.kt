package common

import org.joda.time.LocalDateTime

interface Clock {
    fun now(): LocalDateTime
}

class ConstantClock(private val startTime: LocalDateTime) : Clock {
    override fun now(): LocalDateTime = startTime
}

object RealTimeClock : Clock {
    override fun now(): LocalDateTime = LocalDateTime.now()
}