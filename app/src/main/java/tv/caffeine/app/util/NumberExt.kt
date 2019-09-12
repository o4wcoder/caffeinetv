package tv.caffeine.app.util

import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

fun Int.toZonedDateTime(): ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(this.toLong()), ZoneId.systemDefault())