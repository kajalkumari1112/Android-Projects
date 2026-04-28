package com.example.cryptopricetracker.presentation.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptopricetracker.domain.usecase.*
import com.example.cryptopricetracker.widget.WidgetRefreshHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val getAllCollections: GetAllCollectionsUseCase,
    private val deleteCollection: DeleteCollectionUseCase,
    private val removeCoinFromCollection: RemoveCoinFromCollectionUseCase,
    private val pinCollectionAsWidget: PinCollectionAsWidgetUseCase,
    private val createCollection: CreateCollectionUseCase,
    private val widgetRefreshHelper: WidgetRefreshHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<String>()
    val effect: SharedFlow<String> = _effect.asSharedFlow()

    init {
        getAllCollections()
            .onEach { collections ->
                _uiState.update { it.copy(collections = collections, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Syncs Room's widget_id column against actually-alive home-screen widgets.
     * Call this when the widget bottom sheet opens, or when the collections screen resumes,
     * so stale "Already added" labels are cleared if the user removed the widget externally.
     */
    fun syncWidgetState() {
        viewModelScope.launch {
            widgetRefreshHelper.syncWidgetState()
        }
    }

    fun onEvent(event: CollectionsEvent) {
        when (event) {
            is CollectionsEvent.DeleteCollection -> viewModelScope.launch {
                // ✅ FIX 3: Find the widgetId BEFORE deleting so we can clean up
                val collection = _uiState.value.collections.firstOrNull { it.id == event.collectionId }
                val widgetId = collection?.widgetId

                deleteCollection(event.collectionId)

                // Clean up widget mapping and refresh the widget
                widgetRefreshHelper.onCollectionDeleted(event.collectionId, widgetId)

                _effect.emit("Collection deleted.")
            }
            is CollectionsEvent.RemoveCoin -> viewModelScope.launch {
                removeCoinFromCollection(event.collectionId, event.coinId)
                widgetRefreshHelper.refreshAll()
            }
            is CollectionsEvent.PinAsWidget -> viewModelScope.launch {
                pinCollectionAsWidget(event.collectionId, event.widgetId)
                _effect.emit("Collection pinned as widget!")
                widgetRefreshHelper.refreshAll()
            }
            is CollectionsEvent.CreateCollection -> viewModelScope.launch {
                createCollection(event.name)
                _effect.emit("Collection '${event.name}' created!")
            }
        }
    }
}
