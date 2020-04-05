package manager

import org.joda.time.LocalDateTime


data class User(val uid: Int, val subscriptionUntil: LocalDateTime?)
