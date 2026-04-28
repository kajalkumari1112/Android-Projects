package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCollectionWithCoinsUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    operator fun invoke(collectionId: Long): Flow<CoinCollection?> =
        repository.getCollectionWithCoins(collectionId)
}

