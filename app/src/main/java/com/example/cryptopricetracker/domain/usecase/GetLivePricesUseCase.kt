package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLivePricesUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    operator fun invoke(symbols: List<String>): Flow<Map<String, Double>> =
        repository.getLivePrices(symbols)
}

