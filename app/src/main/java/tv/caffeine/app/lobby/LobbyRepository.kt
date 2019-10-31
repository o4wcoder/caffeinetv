package tv.caffeine.app.lobby

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.toDeferred
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.lobby.type.Page
import tv.caffeine.app.stage.asCaffeineResult
import java.lang.Exception
import javax.inject.Inject

class LobbyRepository @Inject constructor(
    @VisibleForTesting val apolloClient: ApolloClient
) {
    suspend fun loadLobbyV5(page: Page): CaffeineResult<LobbyQuery.Data> {
        return try {
            apolloClient.query(LobbyQuery(page)).toDeferred().await().asCaffeineResult()
        } catch (e: Exception) {
            CaffeineResult.Failure(e)
        }
    }

    suspend fun loadLobbyDetail(cardId: String): CaffeineResult<DetailPageQuery.Data> {
        return try {
            apolloClient.query(DetailPageQuery(cardId)).toDeferred().await().asCaffeineResult()
        } catch (e: Exception) {
            CaffeineResult.Failure(e)
        }
    }
}
