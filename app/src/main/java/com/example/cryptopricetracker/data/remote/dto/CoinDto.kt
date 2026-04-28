package com.example.cryptopricetracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CoinDto(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String?,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("market_cap") val marketCap: Double?,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("price_change_24h") val priceChange24h: Double?,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: Double?,
    @SerializedName("total_volume") val totalVolume: Double?
)

