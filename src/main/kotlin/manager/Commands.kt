package manager

import com.github.jasync.sql.db.SuspendingConnection
import common.Clock
import common.CommonDao
import common.SuspendProcessor
import org.joda.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

sealed class Command

object NewUserCommand : Command()

data class SubscriptionRenewalCommand(val uid: Int, val until: LocalDateTime) : Command()

interface CommandDao {
    suspend fun registerNewUser(): Int

    suspend fun subscriptionRenewal(uid: Int, until: LocalDateTime)
}
class CommandDaoImpl(
    private val connection: SuspendingConnection,
    private val clock: Clock,
    private val poolSize: Int = 10
) : CommonDao(),
    CommandDao {
    data class AvailableUids(val maxUsedUid: Int, val maxAvailableUid: Int)

    private val availableUidsRef: AtomicReference<AvailableUids> =
        AtomicReference(AvailableUids(-1, -1))

    private suspend fun getNewUid(transactionConnection: SuspendingConnection): Int {
        while (true) {
            val availableUids = availableUidsRef.get()
            if (availableUids.maxUsedUid == availableUids.maxAvailableUid) {
                // Доступный пул userId'ов исчерпан
                val curMaxUid = if (availableUids.maxUsedUid == -1) {
                    // Приложение только что запущено и нужно узнать, какой максимальный userId в БД
                    transactionConnection.sendQuery(getMaxUidQuery).rows[0].getInt("max_id")!!
                } else {
                    availableUids.maxAvailableUid
                }
                val nextMaxUid = curMaxUid + poolSize
                // Пополняем пул
                val result =
                    transactionConnection.sendPreparedStatement(changeMaxUidCommand, listOf(nextMaxUid, curMaxUid))
                // Если не получилось обновить пул в БД, продолжаем пытаться
                // (безопасно, т.к. изменений в базу внесено не было)
                if (result.rowsAffected != 1L) {
                    continue
                }
                val resultId = curMaxUid + 1 // Теперь это самый большой из используемых userId'ов
                // Если не получилось обновить пул так как пул был обновлён другим потоком, выдаём ошибку
                // и откатываем все изменения в БД
                if (!availableUidsRef.compareAndSet(availableUids,
                        AvailableUids(resultId, nextMaxUid)
                    )) {
                    throw IllegalStateException("Max UID was changed concurrently")
                }
                return resultId
            } else {
                // пробуем взять userId из незакончившегося пула
                val resultId = availableUids.maxUsedUid + 1
                val newAvailableUids =
                    AvailableUids(
                        resultId,
                        availableUids.maxAvailableUid
                    )
                if (availableUidsRef.compareAndSet(availableUids, newAvailableUids)) {
                    return resultId
                }
            }
        }
    }

    override suspend fun registerNewUser(): Int = connection.inTransaction {
        val newUid = getNewUid(it)
        it.sendPreparedStatement(newUserEventCommand, listOf(newUid))
        newUid
    }

    override suspend fun subscriptionRenewal(uid: Int, until: LocalDateTime) = connection.inTransaction {
        val curDate = clock.now()
        if (!curDate.isBefore(until)) {
            throw IllegalArgumentException("Cannot processOrError renewal until $until at $curDate")
        }
        if (!doesUserExist(uid, it)) {
            throw IllegalArgumentException("User with userId $uid doesn't exist")
        }
        val maxSubscriptionDate = getMaxSubscriptionDate(uid, it)
        if (maxSubscriptionDate != null) {
            if (!maxSubscriptionDate.isBefore(until)) {
                throw IllegalArgumentException(
                    "Cannot processOrError renewal until $until, because current renewal is until $maxSubscriptionDate"
                )
            }
        }
        val maxUserEventId = getMaxUserEventId(uid, it)
        val curUserEventId = maxUserEventId + 1
        it.sendPreparedStatement(newEventCommand, listOf(uid, curUserEventId))
        it.sendPreparedStatement(renewalCommand, listOf(uid, curUserEventId, until))
        Unit
    }

    companion object {
        val getMaxUidQuery = "select MaxUserId.max_id from MaxUserId"

        val renewalCommand =
            "insert into ReleaseSubscriptionEvents (user_id, user_event_id, end_date) values (?, ?, ?);"

        val newEventCommand = "insert into Events (user_id, user_event_id) values (?, ?);"

        val newUserEventCommand = "insert into Events (user_id, user_event_id) values (?, 0);"

        val changeMaxUidCommand = "update MaxUserId set max_id = ? and max_id = ?;"
    }
}

class CommandSuspendProcessor(private val commandDao: CommandDao) :
    SuspendProcessor<Command> {
    override suspend fun process(cmd: Command): String {
        return when (cmd) {
            NewUserCommand -> {
                val newUid = commandDao.registerNewUser()
                "New UID = $newUid"
            }
            is SubscriptionRenewalCommand -> {
                commandDao.subscriptionRenewal(cmd.uid, cmd.until)
                "Successful renewal"
            }
        }
    }
}