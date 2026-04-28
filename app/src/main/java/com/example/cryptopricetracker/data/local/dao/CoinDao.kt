package com.example.cryptopricetracker.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.example.cryptopricetracker.data.local.entity.CoinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {

    /** Returns a PagingSource — Room + Paging 3 auto-generates the implementation. */
    @Query("SELECT * FROM coins ORDER BY position ASC")
    fun pagingSource(): PagingSource<Int, CoinEntity>

    @Query("SELECT * FROM coins WHERE id = :coinId")
    fun getCoinById(coinId: String): Flow<CoinEntity?>

    @Query("SELECT * FROM coins WHERE id IN (:coinIds)")
    fun getCoinsByIds(coinIds: List<String>): Flow<List<CoinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(coins: List<CoinEntity>)

    /** Called during REFRESH — wipes slate clean before re-inserting page 1. */
    @Query("DELETE FROM coins")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM coins")
    suspend fun count(): Int
}

