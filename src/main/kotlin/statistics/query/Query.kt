package statistics.query

sealed class Query

data class GetUserStatsQuery(val uid: Int) : Query()
