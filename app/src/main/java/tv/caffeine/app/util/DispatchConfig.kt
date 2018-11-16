package tv.caffeine.app.util

import kotlinx.coroutines.CoroutineDispatcher

class DispatchConfig(val main: CoroutineDispatcher, val io: CoroutineDispatcher)
