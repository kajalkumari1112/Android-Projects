package com.example.cryptopricetracker.domain.usecase

import app.cash.turbine.test
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.domain.repository.CollectionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAllCollectionsUseCaseTest {

    private val repository = mockk<CollectionRepository>()
    private val useCase = GetAllCollectionsUseCase(repository)

    @Test
    fun `invoke returns collections from repository`() = runTest {
        val fakeCollections = listOf(
            CoinCollection(id = 1L, name = "My Alts", createdAt = 0L, widgetId = null, coins = emptyList()),
            CoinCollection(id = 2L, name = "DeFi", createdAt = 0L, widgetId = null, coins = emptyList())
        )
        every { repository.getAllCollectionsWithCoins() } returns flowOf(fakeCollections)

        useCase().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("My Alts", result[0].name)
            awaitComplete()
        }
    }

    @Test
    fun `invoke returns empty list when no collections exist`() = runTest {
        every { repository.getAllCollectionsWithCoins() } returns flowOf(emptyList())

        useCase().test {
            val result = awaitItem()
            assertEquals(0, result.size)
            awaitComplete()
        }
    }
}

