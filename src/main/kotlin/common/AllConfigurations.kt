package common

import com.typesafe.config.Config

interface ApiConfig {
    val port: Int
}

class ApiConfigImpl(conf: Config) : ApiConfig {
    override val port: Int = conf.getInt("port")
}

interface DatabaseConfig {
    val host: String
    val port: Int
    val database: String
    val username: String
    val password: String
    val maxActiveConnections: Int
}

class DatabaseConfigImpl(conf: Config) : DatabaseConfig {
    override val host: String = conf.getString("host")
    override val port: Int = conf.getInt("port")
    override val database: String = conf.getString("database")
    override val username: String = conf.getString("username")
    override val password: String = conf.getString("password")
    override val maxActiveConnections: Int = conf.getInt("maxActiveConnections")
}

interface StatsConfig {
    val host: String
    val port: Int
    val schema: String
}

class StatsConfigImpl(conf: Config) : StatsConfig {
    override val host: String = conf.getString("host")
    override val port: Int = conf.getInt("port")
    override val schema: String = conf.getString("schema")
}


interface AbstractApplicationConfig {
    val apiConfig: ApiConfig
    val databaseConfig: DatabaseConfig
    val statsConfig: StatsConfig
}