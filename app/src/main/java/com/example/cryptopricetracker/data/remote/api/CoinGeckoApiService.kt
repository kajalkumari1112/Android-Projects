package com.example.cryptopricetracker.data.remote.api

import com.example.cryptopricetracker.data.remote.dto.CoinDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoApiService {

    /**
     * Fetches paginated coin market data.
     * CoinGecko free tier: 25 req/min. We use page + per_page for offset pagination.
     */
    @GET("coins/markets")
    suspend fun getCoins(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 25,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
        @Query("price_change_percentage") priceChangePercentage: String = "24h"
    ): List<CoinDto>
}

