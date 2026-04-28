package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCollectionIdsForCoinUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    operator fun invoke(coinId: String): Flow<List<Long>> =
        repository.getCollectionIdsForCoin(coinId)
}

