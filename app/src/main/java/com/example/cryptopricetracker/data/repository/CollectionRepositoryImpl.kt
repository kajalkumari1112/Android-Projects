package com.example.cryptopricetracker.data.repository

import com.example.cryptopricetracker.data.local.CryptoDatabase
import com.example.cryptopricetracker.data.local.entity.CollectionCoinEntity
import com.example.cryptopricetracker.data.local.entity.CollectionEntity
import com.example.cryptopricetracker.data.mapper.toDomain
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val db: CryptoDatabase
) : CollectionRepository {

    private val dao = db.collectionDao()

    override fun getAllCollectionsWithCoins(): Flow<List<CoinCollection>> =
        dao.getAllCollectionsWithCoins().map { list -> list.map { it.toDomain() } }

    override fun getCollectionWithCoins(collectionId: Long): Flow<CoinCollection?> =
        dao.getCollectionWithCoins(collectionId).map { it?.toDomain() }

    override fun getCollectionIdsForCoin(coinId: String): Flow<List<Long>> =
        dao.getCollectionIdsForCoin(coinId)

    override suspend fun createCollection(name: String): Long =
        dao.insertCollection(CollectionEntity(name = name))

    override suspend fun deleteCollection(collectionId: Long) =
        dao.deleteCollection(CollectionEntity(id = collectionId, name = ""))

    override suspend fun addCoinToCollection(collectionId: Long, coinId: String) =
        dao.addCoinToCollection(CollectionCoinEntity(collectionId = collectionId, coinId = coinId))

    override suspend fun removeCoinFromCollection(collectionId: Long, coinId: String) =
        dao.removeCoinFromCollection(collectionId, coinId)

    override suspend fun setWidgetForCollection(collectionId: Long, widgetId: Int) =
        dao.setWidgetId(collectionId, widgetId)

    override suspend fun clearWidgetId(widgetId: Int) =
        dao.clearWidgetId(widgetId)

    override suspend fun getCollectionByWidgetId(widgetId: Int): CoinCollection? =
        dao.getCollectionByWidgetId(widgetId)?.toDomain()
}

