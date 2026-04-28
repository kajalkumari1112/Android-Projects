package com.example.cryptopricetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the next/prev page keys for each coin entry.
 * Required by Paging 3 RemoteMediator to know where to fetch next.
 */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val coinId: String,
    @ColumnInfo(name = "prev_page") val prevPage: Int?,
    @ColumnInfo(name = "next_page") val nextPage: Int?
)

