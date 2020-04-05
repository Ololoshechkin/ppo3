package statistics

import com.typesafe.config.Config
import common.*

class StatisticsConfig(conf: Config) : AbstractApplicationConfig {
    override val apiConfig: ApiConfig = ApiConfigImpl(conf.getConfig("api"))
    override val databaseConfig: DatabaseConfig = DatabaseConfigImpl(conf.getConfig("database"))
    override val statsConfig: StatsConfig
        get() = throw NotImplementedError("statistics config makes no sense for StatisticsConfig")
}
