package com.example.cryptopricetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cryptopricetracker.data.local.dao.CoinDao
import com.example.cryptopricetracker.data.local.dao.CollectionDao
import com.example.cryptopricetracker.data.local.dao.RemoteKeyDao
import com.example.cryptopricetracker.data.local.entity.*

@Database(
    entities = [
        CoinEntity::class,
        RemoteKeyEntity::class,
        CollectionEntity::class,
        CollectionCoinEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun coinDao(): CoinDao
    abstract fun remoteKeyDao(): RemoteKeyDao
    abstract fun collectionDao(): CollectionDao
}

