package statistics.query

import statistics.model.UserStats

interface QueryableStats {
    fun getUserStats(uid: Int): UserStats?
}
