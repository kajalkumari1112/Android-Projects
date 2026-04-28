package com.example.cryptopricetracker.presentation.coinlist

import com.example.cryptopricetracker.domain.model.CoinCollection

enum class CoinFilter { ALL, TOP_GAINERS, LOSERS, WATCHLIST }

data class CoinListUiState(
    val livePrices: Map<String, Double> = emptyMap(),
    val collections: List<CoinCollection> = emptyList(),
    val coinCollectionIds: Map<String, List<Long>> = emptyMap(),
    val searchQuery: String = "",
    val activeFilter: CoinFilter = CoinFilter.ALL
)

sealed class CoinListEvent {
    data class AddToCollection(val coinId: String, val collectionId: Long) : CoinListEvent()
    data class RemoveFromCollection(val coinId: String, val collectionId: Long) : CoinListEvent()
    data class CreateCollectionAndAdd(val coinId: String, val collectionName: String) : CoinListEvent()
    data class SubscribePrices(val symbols: List<String>) : CoinListEvent()
    data class SearchQueryChanged(val query: String) : CoinListEvent()
    data class FilterChanged(val filter: CoinFilter) : CoinListEvent()
}
