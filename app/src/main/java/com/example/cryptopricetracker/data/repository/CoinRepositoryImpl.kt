package com.example.cryptopricetracker.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.cryptopricetracker.data.local.CryptoDatabase
import com.example.cryptopricetracker.data.mapper.toDomain
import com.example.cryptopricetracker.data.paging.CoinRemoteMediator
import com.example.cryptopricetracker.data.remote.websocket.BinanceWebSocketManager
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepositoryImpl @Inject constructor(
    private val db: CryptoDatabase,
    private val remoteMediator: CoinRemoteMediator,
    private val webSocketManager: BinanceWebSocketManager
) : CoinRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedCoins(): Flow<PagingData<Coin>> = Pager(
        config = PagingConfig(
            pageSize = 50,
            prefetchDistance = 10,       // Fetch next page when 10 items from the end
            initialLoadSize = 50,        // First load = 1 page = 1 API call
            enablePlaceholders = false
        ),
        remoteMediator = remoteMediator,
        pagingSourceFactory = { db.coinDao().pagingSource() }
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun getLivePrices(symbols: List<String>): Flow<Map<String, Double>> {
        // Accumulate price ticks into a running map
        val priceMap = MutableStateFlow<Map<String, Double>>(emptyMap())
        return priceMap
            .onStart {
                webSocketManager.subscribe(symbols)
                // Collect WS updates into the shared state
                webSocketManager.priceUpdates.collect { update ->
                    priceMap.value = priceMap.value + (update.symbol to update.price)
                }
            }
    }
}
