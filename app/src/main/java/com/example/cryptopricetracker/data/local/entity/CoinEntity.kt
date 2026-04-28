package com.example.cryptopricetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a cached coin from the CoinGecko REST API.
 * Used as the single source of truth for the Paging 3 list.
 */
@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val name: String,
    val image: String?,
    @ColumnInfo(name = "current_price") val currentPrice: Double,
    @ColumnInfo(name = "market_cap") val marketCap: Double,
    @ColumnInfo(name = "market_cap_rank") val marketCapRank: Int?,
    @ColumnInfo(name = "price_change_24h") val priceChange24h: Double?,
    @ColumnInfo(name = "price_change_percentage_24h") val priceChangePercentage24h: Double?,
    @ColumnInfo(name = "total_volume") val totalVolume: Double?,
    @ColumnInfo(name = "page_number") val pageNumber: Int, // which page this item came from
    val position: Int                                       // absolute position for ordering
)

