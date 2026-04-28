package com.example.cryptopricetracker.domain.model

data class CoinCollection(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val widgetId: Int?,
    val coins: List<Coin>
) {
    val isPinnedAsWidget: Boolean get() = widgetId != null
}

