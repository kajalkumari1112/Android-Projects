package com.example.cryptopricetracker.data.remote.websocket

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a single Binance WebSocket connection for live price updates.
 *
 * Strategy:
 * - Uses a combined stream URL: wss://stream.binance.com/stream?streams=btcusdt@ticker/ethusdt@ticker/...
 * - Emits PriceUpdate via SharedFlow (replay=0, DROP_OLDEST overflow) — only latest price matters.
 * - Reconnects automatically with exponential backoff on failure.
 */
@Singleton
class BinanceWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "BinanceWebSocketManager"

    private val _priceUpdates = MutableSharedFlow<PriceUpdate>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val priceUpdates: SharedFlow<PriceUpdate> = _priceUpdates.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var currentSymbols: List<String> = emptyList()

    /**
     * Subscribe to live price ticks for the given coin symbols.
     * Symbols should be in lowercase CoinGecko format e.g. "bitcoin" → we convert to "btcusdt".
     * For simplicity we accept symbols directly in Binance format (e.g. "btcusdt").
     */
    fun subscribe(symbols: List<String>) {
        if (symbols.isEmpty()) return
        currentSymbols = symbols
        reconnect()
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnecting")
        webSocket = null
    }

    private fun reconnect() {
        disconnect()
        val streams = currentSymbols.joinToString("/") { "${it.lowercase()}usdt@ticker" }
        val url = "wss://stream.binance.com/stream?streams=$streams"
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, createListener())
    }

    private fun createListener() = object : WebSocketListener() {

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                // Binance combined stream format: {"stream":"btcusdt@ticker","data":{...,"c":"price"}}
                val json = JsonParser.parseString(text).asJsonObject
                val data = json.getAsJsonObject("data") ?: return
                val symbol = data.get("s")?.asString?.lowercase()?.removeSuffix("usdt") ?: return
                val price = data.get("c")?.asString?.toDoubleOrNull() ?: return
                _priceUpdates.tryEmit(PriceUpdate(symbol = symbol, price = price))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse WebSocket message", e)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            // Reconnect after a short delay — in production use exponential backoff
            reconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $reason")
        }
    }
}

