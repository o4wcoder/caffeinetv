package tv.caffeine.app.graphql

import com.apollographql.apollo.ApolloSubscriptionCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowViaChannel

@FlowPreview
fun <T> ApolloSubscriptionCall<T>.asFlow(): Flow<Response<T>> = flowViaChannel { channel ->

    channel.invokeOnClose {
        cancel()
    }
    execute(object : ApolloSubscriptionCall.Callback<T> {
        override fun onConnected() {
        }

        override fun onResponse(response: Response<T>) {
            channel.offer(response)
        }

        override fun onFailure(e: ApolloException) {
            channel.close(e)
        }

        override fun onCompleted() {
            channel.close()
        }

        override fun onTerminated() {
            channel.close()
        }
    })
}
