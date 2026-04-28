package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.repository.CollectionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoveCoinFromCollectionUseCaseTest {

    private val repository = mockk<CollectionRepository>(relaxed = true)
    private val useCase = RemoveCoinFromCollectionUseCase(repository)

    @Test
    fun `invoke delegates to repository with correct args`() = runTest {
        useCase(collectionId = 2L, coinId = "ethereum")
        coVerify(exactly = 1) { repository.removeCoinFromCollection(2L, "ethereum") }
    }
}

