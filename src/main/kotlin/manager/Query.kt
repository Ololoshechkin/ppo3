package manager

import com.github.jasync.sql.db.SuspendingConnection
import common.CommonDao
import common.SuspendProcessor

data class GetUserQuery(val userId: Int)

interface QueryDao {
    suspend fun getUser(uid: Int): User?
}

class QueryDaoImpl(private val connection: SuspendingConnection) : CommonDao(), QueryDao {
    override suspend fun getUser(uid: Int): User? = connection.inTransaction {
        if (!doesUserExist(uid, it)) {
            null
        } else {
            User(uid, getMaxSubscriptionDate(uid, it))
        }
    }
}

class QuerySuspendProcessor(private val queryDao: QueryDao) : SuspendProcessor<GetUserQuery> {
    override suspend fun process(cmd: GetUserQuery) =
        queryDao.getUser(cmd.userId)?.toString() ?: "User with id=${cmd.userId} not found"
}