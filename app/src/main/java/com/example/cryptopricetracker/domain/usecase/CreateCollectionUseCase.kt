package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.repository.CollectionRepository
import javax.inject.Inject

class CreateCollectionUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(name: String): Long = repository.createCollection(name)
}

