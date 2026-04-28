package com.example.cryptopricetracker.domain.repository

import androidx.paging.PagingData
import com.example.cryptopricetracker.domain.model.Coin
import kotlinx.coroutines.flow.Flow

interface CoinRepository {
    /** Returns a Flow of PagingData backed by Room + RemoteMediator. */
    fun getPagedCoins(): Flow<PagingData<Coin>>

    /** Returns live price updates from Binance WebSocket as a Flow of symbol→price map. */
    fun getLivePrices(symbols: List<String>): Flow<Map<String, Double>>
}

