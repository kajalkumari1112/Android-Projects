package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.repository.CollectionRepository
import javax.inject.Inject

class RemoveCoinFromCollectionUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(collectionId: Long, coinId: String) =
        repository.removeCoinFromCollection(collectionId, coinId)
}

