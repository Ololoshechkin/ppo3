package manager

import com.typesafe.config.Config
import common.*

class ManagerConfig(conf: Config) : ApplicationConfig {
    override val apiConfig: ApiConfig = ApiConfigImpl(conf.getConfig("api"))
    override val databaseConfig: DatabaseConfig = DatabaseConfigImpl(conf.getConfig("database"))
    override val statsConfig: StatsConfig
        get() = throw NotImplementedError("statistics config makes no sense fot ManagerConfig")
}
