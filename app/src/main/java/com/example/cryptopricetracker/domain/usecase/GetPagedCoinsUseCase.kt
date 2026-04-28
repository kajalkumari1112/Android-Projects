package com.example.cryptopricetracker.domain.usecase

import androidx.paging.PagingData
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPagedCoinsUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    operator fun invoke(): Flow<PagingData<Coin>> = repository.getPagedCoins()
}

