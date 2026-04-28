package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.repository.CollectionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddCoinToCollectionUseCaseTest {

    private val repository = mockk<CollectionRepository>(relaxed = true)
    private val useCase = AddCoinToCollectionUseCase(repository)

    @Test
    fun `invoke delegates to repository with correct args`() = runTest {
        useCase(collectionId = 1L, coinId = "bitcoin")
        coVerify(exactly = 1) { repository.addCoinToCollection(1L, "bitcoin") }
    }
}

