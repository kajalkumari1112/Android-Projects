package com.example.cryptopricetracker.data.remote.websocket

/**
 * Represents a single price tick received from the Binance WebSocket stream.
 */
data class PriceUpdate(
    val symbol: String,  // e.g. "btcusdt"
    val price: Double
)

