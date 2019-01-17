package tv.caffeine.app.util

import tv.caffeine.app.stage.StageFragmentArgs

fun StageFragmentArgs.broadcasterUsername() = broadcastLink.substringBefore('?').substringBefore('/')

