package tv.caffeine.app.graphql

import com.apollographql.apollo.ApolloSubscriptionCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

@ExperimentalCoroutinesApi
fun <T> ApolloSubscriptionCall<T>.asFlow(): Flow<Response<T>> = callbackFlow {

    execute(object : ApolloSubscriptionCall.Callback<T> {
        override fun onConnected() {
        }

        override fun onResponse(response: Response<T>) {
            try {
                offer(response)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        override fun onFailure(e: ApolloException) {
            cancel(CancellationException("Apollo Error", e))
        }

        override fun onCompleted() {
            channel.close()
        }

        override fun onTerminated() {
            channel.close()
        }
    })
    awaitClose {
        val apolloSubscriptionCall = this@asFlow
        apolloSubscriptionCall.cancel()
    }
}
