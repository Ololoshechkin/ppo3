package gate

import com.github.jasync.sql.db.SuspendingConnection
import common.CommonDao
import org.joda.time.LocalDateTime

sealed class GymCommand(val userId: Int, val timestamp: LocalDateTime) {
    override fun toString() = "${this::class.java.name}($userId, $timestamp)"

    class EnterGymCommand(userId: Int, timestamp: LocalDateTime) : GymCommand(userId, timestamp)

    class ExitGymCommand(userId: Int, timestamp: LocalDateTime) : GymCommand(userId, timestamp)
}

interface GymCommandDao {
    suspend fun processGymComand(cmd: GymCommand): Any?
}

@Suppress("IMPLICIT_CAST_TO_ANY")
class GymCommandDaoImpl(private val connection: SuspendingConnection) : CommonDao(), GymCommandDao {
    private suspend fun getLastUserGateEventType(
        uid: Int,
        transactionConnection: SuspendingConnection
    ): Event? {
        val result = transactionConnection.sendPreparedStatement(lastGateEventQuery, listOf(uid)).rows
        return if (result.size == 0) {
            null
        } else {
            val eventType = GateEventType.valueOf(result[0].getString("gate_event_type")!!)
            val eventTimestamp = result[0].getAs<LocalDateTime>("event_timestamp")
            Event(eventType, eventTimestamp)
        }
    }

    private suspend fun addGateEvent(
        uid: Int,
        eventType: GateEventType,
        eventTimestamp: LocalDateTime,
        transactionConnection: SuspendingConnection
    ) {
        val maxEventId = getMaxUserEventId(uid, transactionConnection)
        val curEventId = maxEventId + 1
        transactionConnection.sendPreparedStatement(newEventCommand, listOf(uid, curEventId))
        transactionConnection
            .sendPreparedStatement(newGateEventCommand, listOf(uid, curEventId, eventType, eventTimestamp))
    }

    override suspend fun processGymComand(cmd: GymCommand) = when (cmd) {
        is GymCommand.EnterGymCommand -> connection.inTransaction {
            if (!doesUserExist(cmd.userId, it)) {
                throw IllegalArgumentException("No user with id = ${cmd.userId}")
            }
            val subscriptionDate = getMaxSubscriptionDate(cmd.userId, it)
            if (subscriptionDate == null || !cmd.timestamp.isBefore(subscriptionDate)) {
                throw IllegalArgumentException(
                    "User (id=${cmd.userId} can't enter at ${cmd.timestamp}"
                )
            }
            val prevEvent = getLastUserGateEventType(cmd.userId, it)
            if (prevEvent?.type == GateEventType.ENTER) {
                throw IllegalArgumentException("User (id = ${cmd.userId}) has already entered gym")
            }
            addGateEvent(cmd.userId, GateEventType.ENTER, cmd.timestamp, it)
        }
        is GymCommand.ExitGymCommand -> connection.inTransaction {
            val prevEvent = getLastUserGateEventType(cmd.userId, it)
            if (prevEvent?.type != GateEventType.ENTER) {
                throw IllegalArgumentException("User (id = ${cmd.userId}) is not in the gym now")
            }
            addGateEvent(cmd.userId, GateEventType.EXIT, cmd.timestamp, it)
            prevEvent.timestamp
        }
    }

    companion object {
        val lastGateEventQuery =
            """
                with CurUserGateIOEvents as (
                    select GateIOEvents.user_event_id,
                           GateIOEvents.gate_event_type,
                           GateIOEvents.event_timestamp
                    from GateIOEvents
                    where GateIOEvents.user_id = ?
                )
                select CurUserGateIOEvents.gate_event_type,
                       CurUserGateIOEvents.event_timestamp
                from CurUserGateIOEvents
                where CurUserGateIOEvents.user_event_id = (
                    select max(CurUserGateIOEvents.user_event_id)
                    from CurUserGateIOEvents
                );
            """.trimIndent()

        val newEventCommand =
            """
                insert into Events (user_id, user_event_id)
                values (?, ?);
            """.trimIndent()

        val newGateEventCommand =
            """
                INSERT INTO GateIOEvents (user_id, user_event_id, gate_event_type, event_timestamp)
                VALUES (?, ?, ?, ?);
            """.trimIndent()
    }
}