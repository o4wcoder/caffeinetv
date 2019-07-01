package tv.caffeine.app.ext

import org.threeten.bp.Clock

fun Clock.seconds() = millis() / 1000
