package statistics.config

import common.ApiConfig
import common.ApiConfigImpl
import common.DatabaseConfig
import common.DatabaseConfigImpl
import com.typesafe.config.Config

interface ApplicationConfig {
    val apiConfig: ApiConfig
    val databaseConfig: DatabaseConfig
}

class ApplicationConfigImpl(conf: Config) : ApplicationConfig {
    override val apiConfig: ApiConfig = ApiConfigImpl(conf.getConfig("api"))
    override val databaseConfig: DatabaseConfig = DatabaseConfigImpl(conf.getConfig("database"))
}
