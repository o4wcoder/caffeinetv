package tv.caffeine.app.net

import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class CaffeineWebSocket(
    private val socket: WebSocket,
    private val sendFilter: (String) -> String
) : WebSocket {
    override fun queueSize(): Long {
        return socket.queueSize()
    }

    override fun send(text: String): Boolean {
        val updatedText = sendFilter(text)
        return socket.send(updatedText)
    }

    override fun send(bytes: ByteString): Boolean {
        return socket.send(bytes)
    }

    override fun close(code: Int, reason: String?): Boolean {
        return socket.close(code, reason)
    }

    override fun cancel() {
        socket.cancel()
    }

    override fun request(): Request {
        return socket.request()
    }
}

class CaffeineWebSocketFactory(
    private val webSocketFactory: WebSocket.Factory
) : WebSocket.Factory {
    override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
        val realSocket = webSocketFactory.newWebSocket(request, CaffeineWebSocketListener(listener))
        return CaffeineWebSocket(realSocket) {
            it.replace("\"StageSubscription\"", "\"Stage\"")
        }
    }
}

class CaffeineWebSocketListener(
    private val listener: WebSocketListener
) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        listener.onOpen(webSocket, response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        listener.onMessage(webSocket, text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        listener.onMessage(webSocket, bytes)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        listener.onClosing(webSocket, code, reason)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        listener.onClosed(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        listener.onFailure(webSocket, t, response)
    }
}
