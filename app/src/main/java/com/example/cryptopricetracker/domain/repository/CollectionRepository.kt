package com.example.cryptopricetracker.domain.repository

import com.example.cryptopricetracker.domain.model.CoinCollection
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun getAllCollectionsWithCoins(): Flow<List<CoinCollection>>
    fun getCollectionWithCoins(collectionId: Long): Flow<CoinCollection?>
    fun getCollectionIdsForCoin(coinId: String): Flow<List<Long>>
    suspend fun createCollection(name: String): Long
    suspend fun deleteCollection(collectionId: Long)
    suspend fun addCoinToCollection(collectionId: Long, coinId: String)
    suspend fun removeCoinFromCollection(collectionId: Long, coinId: String)
    suspend fun setWidgetForCollection(collectionId: Long, widgetId: Int)
    suspend fun clearWidgetId(widgetId: Int)
    suspend fun getCollectionByWidgetId(widgetId: Int): CoinCollection?
}

