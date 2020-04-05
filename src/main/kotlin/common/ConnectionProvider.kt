package common

import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder

interface ConnectionProvider {
    fun getConnection(conf: DatabaseConfig): SuspendingConnection
}

object ConnectionPoolProvider : ConnectionProvider {
    override fun getConnection(conf: DatabaseConfig): SuspendingConnection {
        return PostgreSQLConnectionBuilder.createConnectionPool {
            host = conf.host
            port = conf.port
            database = conf.database
            username = conf.username
            password = conf.password
            maxActiveConnections = conf.maxActiveConnections
        }.asSuspending
    }

}
