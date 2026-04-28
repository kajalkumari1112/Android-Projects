package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.repository.CollectionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateCollectionUseCaseTest {

    private val repository = mockk<CollectionRepository>(relaxed = true)
    private val useCase = CreateCollectionUseCase(repository)

    @Test
    fun `invoke delegates to repository with correct name`() = runTest {
        useCase("DeFi Kings")
        coVerify(exactly = 1) { repository.createCollection("DeFi Kings") }
    }
}

