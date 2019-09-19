package tv.caffeine.app.ext

import org.threeten.bp.ZonedDateTime

fun ZonedDateTime?.isNewer(referenceTimestamp: ZonedDateTime?): Boolean {
    if (this == null || referenceTimestamp == null) return true
    return this.isAfter(referenceTimestamp)
}