package com.example.cryptopricetracker.domain.usecase

import com.example.cryptopricetracker.domain.repository.CollectionRepository
import javax.inject.Inject

class PinCollectionAsWidgetUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(collectionId: Long, widgetId: Int) =
        repository.setWidgetForCollection(collectionId, widgetId)
}

