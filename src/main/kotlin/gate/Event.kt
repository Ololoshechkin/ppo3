package gate

import org.joda.time.LocalDateTime

enum class GateEventType {
    ENTER, EXIT
}

data class Event(val type: GateEventType, val timestamp: LocalDateTime)
