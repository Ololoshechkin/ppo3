package common

import com.github.jasync.sql.db.SuspendingConnection
import org.jetbrains.annotations.Contract
import org.joda.time.LocalDateTime

abstract class CommonCommandDao {
    @Contract("!doesUserExist(userId, connection) -> fail")
    protected suspend fun getMaxUserEventId(uid: Int, connection: SuspendingConnection): Int {
        return connection
            .sendPreparedStatement(maxUserEventIdQuery, listOf(uid))
            .rows[0]
            .getInt("max")!!
    }

    companion object {
        val maxUserEventIdQuery = "select max(Events.user_event_id) from Events where Events.user_id = ?;"
    }
}

abstract class CommonDao : CommonCommandDao() {
    protected suspend fun doesUserExist(uid: Int, connection: SuspendingConnection): Boolean {
        return connection.sendPreparedStatement(getUserQuery, listOf(uid)).rows.size > 0
    }

    protected suspend fun getMaxSubscriptionDate(uid: Int, connection: SuspendingConnection): LocalDateTime? {
        val result = connection.sendPreparedStatement(maxSubscriptionDateQuery, listOf(uid)).rows
        return if (result.size > 0) {
            result[0].getAs<LocalDateTime>("end_date")
        } else {
            null
        }
    }

    companion object {
        val getUserQuery = "select * from Events where Events.user_id = ? limit 1;"

        val maxSubscriptionDateQuery =
            """
                with CurUserEvents as (
                    select ReleaseSubscriptionEvents.user_event_id,
                           ReleaseSubscriptionEvents.end_date
                    from ReleaseSubscriptionEvents
                    where ReleaseSubscriptionEvents.user_id = ?
                )
                select CurUserEvents.end_date
                from CurUserEvents
                where CurUserEvents.user_event_id = (
                    select max(CurUserEvents.user_event_id)
                    from CurUserEvents
                );
            """.trimIndent()
    }
}