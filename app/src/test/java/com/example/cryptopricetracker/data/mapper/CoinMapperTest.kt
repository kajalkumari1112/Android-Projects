package com.example.cryptopricetracker.data.mapper

import com.example.cryptopricetracker.data.local.entity.CoinEntity
import com.example.cryptopricetracker.data.remote.dto.CoinDto
import org.junit.Assert.*
import org.junit.Test

class CoinMapperTest {

    private val dto = CoinDto(
        id = "bitcoin",
        symbol = "btc",
        name = "Bitcoin",
        image = "https://example.com/btc.png",
        currentPrice = 65000.0,
        marketCap = 1_200_000_000_000.0,
        marketCapRank = 1,
        priceChange24h = 1200.0,
        priceChangePercentage24h = 1.88,
        totalVolume = 30_000_000_000.0
    )

    private val entity = CoinEntity(
        id = "bitcoin",
        symbol = "btc",
        name = "Bitcoin",
        image = "https://example.com/btc.png",
        currentPrice = 65000.0,
        marketCap = 1_200_000_000_000.0,
        marketCapRank = 1,
        priceChange24h = 1200.0,
        priceChangePercentage24h = 1.88,
        totalVolume = 30_000_000_000.0,
        pageNumber = 1,
        position = 0
    )

    @Test
    fun `CoinDto toEntity maps all fields correctly`() {
        val result = dto.toEntity(pageNumber = 1, position = 0)
        assertEquals("bitcoin", result.id)
        assertEquals("btc", result.symbol)
        assertEquals("Bitcoin", result.name)
        assertEquals(65000.0, result.currentPrice, 0.0)
        assertEquals(1, result.marketCapRank)
        assertEquals(1, result.pageNumber)
        assertEquals(0, result.position)
    }

    @Test
    fun `CoinDto toEntity uses 0 defaults for null price and marketCap`() {
        val nullDto = dto.copy(currentPrice = null, marketCap = null)
        val result = nullDto.toEntity(pageNumber = 1, position = 0)
        assertEquals(0.0, result.currentPrice, 0.0)
        assertEquals(0.0, result.marketCap, 0.0)
    }

    @Test
    fun `CoinEntity toDomain maps all fields correctly`() {
        val domain = entity.toDomain()
        assertEquals("bitcoin", domain.id)
        assertEquals("btc", domain.symbol)
        assertEquals("Bitcoin", domain.name)
        assertEquals(65000.0, domain.currentPrice, 0.0)
        assertEquals(1.88, domain.priceChangePercentage24h!!, 0.0)
    }

    @Test
    fun `Coin binanceSymbol returns lowercase symbol`() {
        val domain = entity.toDomain()
        assertEquals("btc", domain.binanceSymbol)
    }
}

