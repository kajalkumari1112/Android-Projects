package com.example.cryptopricetracker.data.mapper

import com.example.cryptopricetracker.data.local.entity.CoinEntity
import com.example.cryptopricetracker.data.local.entity.CollectionEntity
import com.example.cryptopricetracker.data.local.relation.CollectionWithCoins
import com.example.cryptopricetracker.data.remote.dto.CoinDto
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.model.CoinCollection

// ── DTO → Entity ─────────────────────────────────────────────────────────────

fun CoinDto.toEntity(pageNumber: Int, position: Int) = CoinEntity(
    id = id,
    symbol = symbol,
    name = name,
    image = image,
    currentPrice = currentPrice ?: 0.0,
    marketCap = marketCap ?: 0.0,
    marketCapRank = marketCapRank,
    priceChange24h = priceChange24h,
    priceChangePercentage24h = priceChangePercentage24h,
    totalVolume = totalVolume,
    pageNumber = pageNumber,
    position = position
)

// ── Entity → Domain Model ────────────────────────────────────────────────────

fun CoinEntity.toDomain() = Coin(
    id = id,
    symbol = symbol,
    name = name,
    imageUrl = image,
    currentPrice = currentPrice,
    marketCap = marketCap,
    marketCapRank = marketCapRank,
    priceChange24h = priceChange24h,
    priceChangePercentage24h = priceChangePercentage24h,
    totalVolume = totalVolume
)

// ── Collection Relation → Domain Model ──────────────────────────────────────

fun CollectionWithCoins.toDomain() = CoinCollection(
    id = collection.id,
    name = collection.name,
    createdAt = collection.createdAt,
    widgetId = collection.widgetId,
    coins = coins.map { it.toDomain() }
)

fun CollectionEntity.toDomain(coins: List<Coin> = emptyList()) = CoinCollection(
    id = id,
    name = name,
    createdAt = createdAt,
    widgetId = widgetId,
    coins = coins
)

