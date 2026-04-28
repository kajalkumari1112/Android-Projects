package com.example.cryptopricetracker.presentation.collections

import com.example.cryptopricetracker.domain.model.CoinCollection

data class CollectionsUiState(
    val collections: List<CoinCollection> = emptyList(),
    val isLoading: Boolean = false
)

sealed class CollectionsEvent {
    data class DeleteCollection(val collectionId: Long) : CollectionsEvent()
    data class RemoveCoin(val collectionId: Long, val coinId: String) : CollectionsEvent()
    data class PinAsWidget(val collectionId: Long, val widgetId: Int) : CollectionsEvent()
}

