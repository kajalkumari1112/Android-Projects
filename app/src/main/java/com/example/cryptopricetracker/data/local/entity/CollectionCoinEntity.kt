package com.example.cryptopricetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table enabling many-to-many relationship between collections and coins.
 * A single coin can belong to multiple collections.
 */
@Entity(
    tableName = "collection_coins",
    primaryKeys = ["collection_id", "coin_id"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE // removing a collection removes its coin mappings
        )
    ],
    indices = [Index("coin_id")] // prevents full table scan when resolving many-to-many relation
)
data class CollectionCoinEntity(
    @ColumnInfo(name = "collection_id") val collectionId: Long,
    @ColumnInfo(name = "coin_id") val coinId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)
