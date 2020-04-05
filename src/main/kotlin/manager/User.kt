package manager

import org.joda.time.LocalDateTime


data class User(val userId: Int, val subscriptionUntil: LocalDateTime?)
