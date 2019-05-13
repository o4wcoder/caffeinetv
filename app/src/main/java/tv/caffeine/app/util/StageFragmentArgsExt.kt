package tv.caffeine.app.util

import tv.caffeine.app.stage.StagePagerFragmentArgs

fun StagePagerFragmentArgs.broadcasterUsername() = broadcastLink.substringBefore('?').substringBefore('/')
