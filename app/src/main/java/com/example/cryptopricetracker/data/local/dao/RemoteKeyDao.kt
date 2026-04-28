package com.example.cryptopricetracker.data.local.dao

import androidx.room.*
import com.example.cryptopricetracker.data.local.entity.RemoteKeyEntity

@Dao
interface RemoteKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<RemoteKeyEntity>)

    @Query("SELECT * FROM remote_keys WHERE coinId = :coinId")
    suspend fun remoteKeyByCoinId(coinId: String): RemoteKeyEntity?

    @Query("DELETE FROM remote_keys")
    suspend fun clearAll()
}

