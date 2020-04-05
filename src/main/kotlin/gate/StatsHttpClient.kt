package gate

import common.StatsConfig
import common.serialize
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import org.joda.time.LocalDateTime

interface StatsHttpClient {
    suspend fun exitCommand(uid: Int, enterTimestamp: LocalDateTime, exitTimestamp: LocalDateTime): String
}

class StatsHttpClientImpl(private val client: HttpClient, private val conf: StatsConfig) : StatsHttpClient {
    override suspend fun exitCommand(uid: Int, enterTimestamp: LocalDateTime, exitTimestamp: LocalDateTime): String {
        val enterTimeString = enterTimestamp.serialize()
        val exitTimeString = exitTimestamp.serialize()
        val url = "${conf.schema}://${conf.host}:${conf.port}/command/exit?" +
                "userId=$uid&enter=$enterTimeString&exit=$exitTimeString"
        return client.get(url)
    }
}

interface StatsHttpClientsProvider {
    fun getClient(): StatsHttpClient
}

class StatsHttpClientsProviderImpl(private val conf: StatsConfig) : StatsHttpClientsProvider {
    override fun getClient(): StatsHttpClient {
        val client = HttpClient()
        return StatsHttpClientImpl(client, conf)
    }
}