package tv.caffeine.app.stage

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.model.CaffeineResult

interface StageDirector {
    @ExperimentalCoroutinesApi
    suspend fun stageConfiguration(username: String, clientId: String): Flow<CaffeineResult<Map<String, NewReyes.Feed>>>
}
