package gate

import com.typesafe.config.Config
import common.*

class GateApplicationConfig(conf: Config) : ApplicationConfig {
    override val apiConfig: ApiConfig = ApiConfigImpl(conf.getConfig("api"))
    override val databaseConfig: DatabaseConfig = DatabaseConfigImpl(conf.getConfig("database"))
    override val statsConfig: StatsConfig = StatsConfigImpl(conf.getConfig("statistics"))
}