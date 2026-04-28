package com.example.cryptopricetracker.domain.model

/**
 * Pure domain model — no Android or framework dependencies.
 * The ViewModel and UI work exclusively with this model.
 */
data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val currentPrice: Double,
    val marketCap: Double,
    val marketCapRank: Int?,
    val priceChange24h: Double?,
    val priceChangePercentage24h: Double?,
    val totalVolume: Double?
) {
    val binanceSymbol: String get() = symbol.lowercase()
}

