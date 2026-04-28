package com.example.cryptopricetracker.data.local.dao

import androidx.room.*
import com.example.cryptopricetracker.data.local.entity.CollectionCoinEntity
import com.example.cryptopricetracker.data.local.entity.CollectionEntity
import com.example.cryptopricetracker.data.local.relation.CollectionWithCoins
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("SELECT * FROM collections ORDER BY created_at DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun getCollectionById(id: Long): Flow<CollectionEntity?>

    /** Returns each collection with the full list of its coins — uses Room relation. */
    @Transaction
    @Query("SELECT * FROM collections ORDER BY created_at DESC")
    fun getAllCollectionsWithCoins(): Flow<List<CollectionWithCoins>>

    @Transaction
    @Query("SELECT * FROM collections WHERE id = :collectionId")
    fun getCollectionWithCoins(collectionId: Long): Flow<CollectionWithCoins?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCoinToCollection(mapping: CollectionCoinEntity)

    @Query("DELETE FROM collection_coins WHERE collection_id = :collectionId AND coin_id = :coinId")
    suspend fun removeCoinFromCollection(collectionId: Long, coinId: String)

    @Query("SELECT collection_id FROM collection_coins WHERE coin_id = :coinId")
    fun getCollectionIdsForCoin(coinId: String): Flow<List<Long>>

    @Query("UPDATE collections SET widget_id = :widgetId WHERE id = :collectionId")
    suspend fun setWidgetId(collectionId: Long, widgetId: Int)

    @Query("SELECT * FROM collections WHERE widget_id = :widgetId LIMIT 1")
    suspend fun getCollectionByWidgetId(widgetId: Int): CollectionEntity?

    @Query("UPDATE collections SET widget_id = NULL WHERE widget_id = :widgetId")
    suspend fun clearWidgetId(widgetId: Int)
}

