package com.example.cryptopricetracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.cryptopricetracker.data.local.entity.CoinEntity
import com.example.cryptopricetracker.data.local.entity.CollectionCoinEntity
import com.example.cryptopricetracker.data.local.entity.CollectionEntity

/**
 * Room many-to-many relation:
 * CollectionEntity (1) <---> CollectionCoinEntity (junction) <---> CoinEntity (M)
 */
data class CollectionWithCoins(
    @Embedded val collection: CollectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CollectionCoinEntity::class,
            parentColumn = "collection_id",
            entityColumn = "coin_id"
        )
    )
    val coins: List<CoinEntity>
)

