package com.example.cryptopricetracker.presentation.coinlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.usecase.*
import com.example.cryptopricetracker.widget.WidgetRefreshHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinListViewModel @Inject constructor(
    private val getPagedCoins: GetPagedCoinsUseCase,
    private val getLivePrices: GetLivePricesUseCase,
    private val getAllCollections: GetAllCollectionsUseCase,
    private val addCoinToCollection: AddCoinToCollectionUseCase,
    private val removeCoinFromCollection: RemoveCoinFromCollectionUseCase,
    private val createCollection: CreateCollectionUseCase,
    private val getCollectionIdsForCoin: GetCollectionIdsForCoinUseCase,
    private val widgetRefreshHelper: WidgetRefreshHelper
) : ViewModel() {

    // ── Paginated coin list (cached to survive config changes) ────────────────
    val pagedCoins: Flow<PagingData<Coin>> = getPagedCoins()
        .cachedIn(viewModelScope)

    // ── UI state (prices, collections, memberships) ───────────────────────────
    private val _uiState = MutableStateFlow(CoinListUiState())
    val uiState: StateFlow<CoinListUiState> = _uiState.asStateFlow()

    // ── One-shot UI events (snackbar messages, etc.) ──────────────────────────
    private val _effect = MutableSharedFlow<String>()
    val effect: SharedFlow<String> = _effect.asSharedFlow()

    init {
        observeCollections()
    }

    private fun observeCollections() {
        getAllCollections()
            .onEach { collections ->
                _uiState.update { it.copy(collections = collections) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Called once the first page of coins is visible on screen.
     * Subscribes to Binance WebSocket for live prices of visible symbols.
     */
    fun subscribeToPrices(symbols: List<String>) {
        if (symbols.isEmpty()) return
        getLivePrices(symbols)
            .onEach { prices -> _uiState.update { it.copy(livePrices = prices) } }
            .launchIn(viewModelScope)
    }

    /** Loads which collections a particular coin belongs to (lazy, on-demand). */
    fun observeCollectionMembership(coinId: String) {
        getCollectionIdsForCoin(coinId)
            .onEach { ids ->
                _uiState.update { state ->
                    state.copy(
                        coinCollectionIds = state.coinCollectionIds + (coinId to ids)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Syncs Room's widget_id column against actually-alive home-screen widgets.
     * Call this when the widget bottom sheet opens so stale "Already added" labels
     * are cleared if the user removed the widget externally.
     */
    fun syncWidgetState() {
        viewModelScope.launch {
            widgetRefreshHelper.syncWidgetState()
        }
    }

    fun onEvent(event: CoinListEvent) {
        when (event) {
            is CoinListEvent.AddToCollection -> viewModelScope.launch {
                addCoinToCollection(event.collectionId, event.coinId)
                _effect.emit("Added to collection!")
                widgetRefreshHelper.refreshAll()
            }
            is CoinListEvent.RemoveFromCollection -> viewModelScope.launch {
                removeCoinFromCollection(event.collectionId, event.coinId)
                _effect.emit("Removed from collection.")
                widgetRefreshHelper.refreshAll()
            }
            is CoinListEvent.CreateCollectionAndAdd -> viewModelScope.launch {
                val id = createCollection(event.collectionName)
                addCoinToCollection(id, event.coinId)
                _effect.emit("Collection '${event.collectionName}' created!")
                widgetRefreshHelper.refreshAll()
            }
            is CoinListEvent.SubscribePrices -> subscribeToPrices(event.symbols)
            is CoinListEvent.SearchQueryChanged -> _uiState.update { it.copy(searchQuery = event.query) }
            is CoinListEvent.FilterChanged -> _uiState.update { it.copy(activeFilter = event.filter) }
        }
    }
}
