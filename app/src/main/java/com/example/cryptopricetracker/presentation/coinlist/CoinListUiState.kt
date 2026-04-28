package com.example.cryptopricetracker.presentation.coinlist

import com.example.cryptopricetracker.domain.model.CoinCollection

data class CoinListUiState(
    val livePrices: Map<String, Double> = emptyMap(),
    val collections: List<CoinCollection> = emptyList(),
    val coinCollectionIds: Map<String, List<Long>> = emptyMap() // coinId → collectionIds it belongs to
)

sealed class CoinListEvent {
    data class AddToCollection(val coinId: String, val collectionId: Long) : CoinListEvent()
    data class RemoveFromCollection(val coinId: String, val collectionId: Long) : CoinListEvent()
    data class CreateCollectionAndAdd(val coinId: String, val collectionName: String) : CoinListEvent()
    data class SubscribePrices(val symbols: List<String>) : CoinListEvent()
}

