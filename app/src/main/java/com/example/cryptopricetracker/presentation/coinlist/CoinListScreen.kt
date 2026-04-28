package com.example.cryptopricetracker.presentation.coinlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.presentation.widget.PinWidgetBottomSheet
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(
    onNavigateToCollections: () -> Unit,
    isExpandedScreen: Boolean = false,
    viewModel: CoinListViewModel = hiltViewModel()
) {
    val pagedCoins = viewModel.pagedCoins.collectAsLazyPagingItems()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWidgetSheet by remember { mutableStateOf(false) }

    // Collect one-shot effects (snackbar messages)
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Subscribe to live prices once coins are loaded
    LaunchedEffect(pagedCoins.itemCount) {
        if (pagedCoins.itemCount > 0) {
            val symbols = (0 until pagedCoins.itemCount)
                .mapNotNull { pagedCoins.peek(it)?.binanceSymbol }
                .distinct()
            viewModel.onEvent(CoinListEvent.SubscribePrices(symbols))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crypto Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    // Collections — bookmark icon
                    IconButton(onClick = onNavigateToCollections) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "My Collections")
                    }
                    // Pin widget — pushpin icon
                    IconButton(onClick = { showWidgetSheet = true }) {
                        Icon(Icons.Default.PushPin, contentDescription = "Add Widget")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = pagedCoins.loadState.refresh is LoadState.Loading,
            onRefresh = { pagedCoins.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CoinPagingList(
                pagedCoins = pagedCoins,
                uiState = uiState,
                onAddToCollection = { coinId, collectionId ->
                    viewModel.onEvent(CoinListEvent.AddToCollection(coinId, collectionId))
                },
                onRemoveFromCollection = { coinId, collectionId ->
                    viewModel.onEvent(CoinListEvent.RemoveFromCollection(coinId, collectionId))
                },
                onCreateCollection = { coinId, name ->
                    viewModel.onEvent(CoinListEvent.CreateCollectionAndAdd(coinId, name))
                },
                onCoinVisible = { coinId -> viewModel.observeCollectionMembership(coinId) },
                onSearchQueryChanged = { viewModel.onEvent(CoinListEvent.SearchQueryChanged(it)) },
                onFilterChanged = { viewModel.onEvent(CoinListEvent.FilterChanged(it)) },
                onSortChanged = { viewModel.onEvent(CoinListEvent.SortChanged(it)) }
            )
        }
    }

    // In-app widget setup sheet (requirement 2c)
    if (showWidgetSheet) {
        PinWidgetBottomSheet(
            collections = uiState.collections,
            onSyncWidgetState = { viewModel.syncWidgetState() },
            onDismiss = { showWidgetSheet = false }
        )
    }
}

// ─── Paging list ──────────────────────────────────────────────────────────────

@Composable
private fun CoinPagingList(
    pagedCoins: LazyPagingItems<Coin>,
    uiState: CoinListUiState,
    onAddToCollection: (String, Long) -> Unit,
    onRemoveFromCollection: (String, Long) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    onCoinVisible: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterChanged: (CoinFilter) -> Unit,
    onSortChanged: (SortBy) -> Unit
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    val allCoins = (0 until pagedCoins.itemCount).mapNotNull { pagedCoins.peek(it) }
    val filtered = remember(allCoins, uiState.searchQuery, uiState.activeFilter, uiState.coinCollectionIds, uiState.livePrices, uiState.sortBy, uiState.sortOrder) {
        allCoins
            .filter { coin ->
                val q = uiState.searchQuery.trim().lowercase()
                q.isEmpty() || coin.name.lowercase().contains(q) || coin.symbol.lowercase().contains(q)
            }
            .let { list ->
                when (uiState.activeFilter) {
                    CoinFilter.ALL -> list
                    CoinFilter.TOP_GAINERS -> list.filter { (it.priceChangePercentage24h ?: 0.0) > 0 }
                        .sortedByDescending { it.priceChangePercentage24h ?: 0.0 }
                    CoinFilter.LOSERS -> list.filter { (it.priceChangePercentage24h ?: 0.0) < 0 }
                        .sortedBy { it.priceChangePercentage24h ?: 0.0 }
                    CoinFilter.WATCHLIST -> list.filter {
                        (uiState.coinCollectionIds[it.id] ?: emptyList()).isNotEmpty()
                    }
                }
            }
            .let { list ->
                // Apply sort only when not already sorted by a filter-specific order
                if (uiState.activeFilter == CoinFilter.ALL || uiState.activeFilter == CoinFilter.WATCHLIST) {
                    val sorted = when (uiState.sortBy) {
                        SortBy.RANK -> list.sortedBy { it.marketCapRank ?: Int.MAX_VALUE }
                        SortBy.PRICE -> list.sortedBy { uiState.livePrices[it.binanceSymbol] ?: it.currentPrice }
                        SortBy.CHANGE -> list.sortedBy { it.priceChangePercentage24h ?: 0.0 }
                    }
                    if (uiState.sortOrder == SortOrder.DESC) sorted.reversed() else sorted
                } else list
            }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Search bar ──────────────────────────────────────────────────────
        item {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                onDone = { focusManager.clearFocus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // ── Filter chips ────────────────────────────────────────────────────
        item {
            FilterChipRow(
                activeFilter = uiState.activeFilter,
                onFilterSelected = onFilterChanged,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Summary row: N COINS | RANK | PRICE | CHANGE ────────────────────
        item {
            SummaryHeader(
                count = filtered.size,
                sortBy = uiState.sortBy,
                sortOrder = uiState.sortOrder,
                onSortChanged = onSortChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ── Coin items ──────────────────────────────────────────────────────
        if (uiState.searchQuery.isNotBlank() || uiState.activeFilter != CoinFilter.ALL) {
            // Filtered view — render from the in-memory list
            items(filtered, key = { it.id }) { coin ->
                LaunchedEffect(coin.id) { onCoinVisible(coin.id) }
                val livePrice = uiState.livePrices[coin.binanceSymbol] ?: coin.currentPrice
                val collectionIds = uiState.coinCollectionIds[coin.id] ?: emptyList()
                CoinListItem(
                    coin = coin,
                    livePrice = livePrice,
                    collections = uiState.collections,
                    coinCollectionIds = collectionIds,
                    onAddToCollection = onAddToCollection,
                    onRemoveFromCollection = onRemoveFromCollection,
                    onCreateCollection = onCreateCollection,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            // Default — use the full paged list
            items(count = pagedCoins.itemCount, key = pagedCoins.itemKey { it.id }) { index ->
                val coin = pagedCoins[index] ?: return@items
                LaunchedEffect(coin.id) { onCoinVisible(coin.id) }
                val livePrice = uiState.livePrices[coin.binanceSymbol] ?: coin.currentPrice
                val collectionIds = uiState.coinCollectionIds[coin.id] ?: emptyList()
                CoinListItem(
                    coin = coin,
                    livePrice = livePrice,
                    collections = uiState.collections,
                    coinCollectionIds = collectionIds,
                    onAddToCollection = onAddToCollection,
                    onRemoveFromCollection = onRemoveFromCollection,
                    onCreateCollection = onCreateCollection,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Loading / error footers
        when (val state = pagedCoins.loadState.append) {
            is LoadState.Loading -> item { LoadingFooter() }
            is LoadState.Error -> item { ErrorFooter(state.error.message) { pagedCoins.retry() } }
            else -> Unit
        }
        when (val state = pagedCoins.loadState.refresh) {
            is LoadState.Error -> item { ErrorContent(state.error.message) { pagedCoins.refresh() } }
            else -> Unit
        }
    }
}

// ─── Search bar ───────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Search coins...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onDone() }),
        modifier = modifier
    )
}

// ─── Filter chips row ─────────────────────────────────────────────────────────

private val filterLabels = listOf(
    CoinFilter.ALL to "All",
    CoinFilter.TOP_GAINERS to "Top Gainers",
    CoinFilter.LOSERS to "Losers",
    CoinFilter.WATCHLIST to "Watchlist"
)

@Composable
private fun FilterChipRow(
    activeFilter: CoinFilter,
    onFilterSelected: (CoinFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filterLabels) { (filter, label) ->
            val selected = filter == activeFilter
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6C3CE1),
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
}

// ─── Summary header ───────────────────────────────────────────────────────────

@Composable
private fun SummaryHeader(
    count: Int,
    sortBy: SortBy,
    sortOrder: SortOrder,
    onSortChanged: (SortBy) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSortColor = Color(0xFF6C3CE1)
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun SortLabel(label: String, column: SortBy) {
        val isActive = sortBy == column
        val arrow = if (isActive) (if (sortOrder == SortOrder.ASC) " ↑" else " ↓") else ""
        Text(
            text = label + arrow,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) activeSortColor else inactiveColor,
            modifier = Modifier
                .clickable { onSortChanged(column) }
                .padding(4.dp)
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$count COINS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = inactiveColor,
            modifier = Modifier.weight(1f)
        )
        SortLabel("RANK", SortBy.RANK)
        Spacer(Modifier.width(8.dp))
        SortLabel("PRICE", SortBy.PRICE)
        Spacer(Modifier.width(8.dp))
        SortLabel("CHANGE", SortBy.CHANGE)
    }
}

// ─── Coin list item ───────────────────────────────────────────────────────────

@Composable
fun CoinListItem(
    coin: Coin,
    livePrice: Double,
    collections: List<CoinCollection>,
    coinCollectionIds: List<Long>,
    onAddToCollection: (String, Long) -> Unit,
    onRemoveFromCollection: (String, Long) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val isInAnyCollection = coinCollectionIds.isNotEmpty()
    val isPositive = (coin.priceChangePercentage24h ?: 0.0) >= 0
    val changeColor = if (isPositive) Color(0xFF00C853) else Color(0xFFD50000)

    val bookmarkTint by animateColorAsState(
        targetValue = if (isInAnyCollection) Color(0xFFF5B800)
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "bookmark_tint"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "${coin.marketCapRank ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            Spacer(Modifier.width(8.dp))

            // Circular icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coin.imageUrl != null) {
                    AsyncImage(
                        model = coin.imageUrl,
                        contentDescription = coin.name,
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = coin.symbol.take(2).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            // Name + symbol · market cap
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${coin.symbol.uppercase()} · \$${formatMarketCap(coin.marketCap)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))

            // Sparkline
            Sparkline(
                isPositive = isPositive,
                color = changeColor,
                modifier = Modifier.size(width = 56.dp, height = 32.dp)
            )
            Spacer(Modifier.width(12.dp))

            // Price + change badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(livePrice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                PriceChangeBadge(coin.priceChangePercentage24h)
            }
            Spacer(Modifier.width(4.dp))

            // Bookmark
            IconButton(onClick = { showBottomSheet = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isInAnyCollection) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Add to collection",
                    tint = bookmarkTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showBottomSheet) {
        AddToCollectionBottomSheet(
            coin = coin,
            collections = collections,
            coinCollectionIds = coinCollectionIds,
            onDismiss = { showBottomSheet = false },
            onAddToCollection = { collectionId ->
                onAddToCollection(coin.id, collectionId)
                showBottomSheet = false
            },
            onRemoveFromCollection = { collectionId -> onRemoveFromCollection(coin.id, collectionId) },
            onCreateCollection = { name ->
                onCreateCollection(coin.id, name)
                showBottomSheet = false
            }
        )
    }
}

// ─── Sparkline ────────────────────────────────────────────────────────────────

@Composable
private fun Sparkline(isPositive: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val xs = listOf(0f, w * 0.25f, w * 0.5f, w * 0.75f, w)
        val ys: List<Float> = if (isPositive)
            listOf(h * 0.75f, h * 0.55f, h * 0.65f, h * 0.35f, h * 0.15f)
        else
            listOf(h * 0.25f, h * 0.45f, h * 0.35f, h * 0.65f, h * 0.85f)

        val fillPath = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until xs.size) lineTo(xs[i], ys[i])
            lineTo(xs.last(), h); lineTo(xs.first(), h); close()
        }
        drawPath(fillPath, color = color.copy(alpha = 0.12f))

        val linePath = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until xs.size) lineTo(xs[i], ys[i])
        }
        drawPath(linePath, color = color, style = Stroke(width = 2.5f))
    }
}

// ─── Price change badge ───────────────────────────────────────────────────────

@Composable
private fun PriceChangeBadge(changePercent: Double?) {
    if (changePercent == null) return
    val isPositive = changePercent >= 0
    val color = if (isPositive) Color(0xFF00C853) else Color(0xFFD50000)
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = "${if (isPositive) "▲" else "▼"} ${"%.2f".format(Math.abs(changePercent))}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

fun formatPrice(price: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    return when {
        price >= 1_000 -> { fmt.maximumFractionDigits = 0; "\$${fmt.format(price)}" }
        price >= 1     -> { fmt.maximumFractionDigits = 2; fmt.minimumFractionDigits = 2; "\$${fmt.format(price)}" }
        else           -> { fmt.maximumFractionDigits = 4; fmt.minimumFractionDigits = 4; "\$${fmt.format(price)}" }
    }
}

private fun formatMarketCap(value: Double): String = when {
    value >= 1_000_000_000_000 -> "${"%.1f".format(value / 1_000_000_000_000)}T"
    value >= 1_000_000_000     -> "${"%.0f".format(value / 1_000_000_000)}B"
    value >= 1_000_000         -> "${"%.0f".format(value / 1_000_000)}M"
    else                       -> value.toLong().toString()
}

// ─── Add to collection bottom sheet ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToCollectionBottomSheet(
    coin: Coin,
    collections: List<CoinCollection>,
    coinCollectionIds: List<Long>,
    onDismiss: () -> Unit,
    onAddToCollection: (Long) -> Unit,
    onRemoveFromCollection: (Long) -> Unit,
    onCreateCollection: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add ${coin.name} to collection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (collections.isEmpty()) {
                Text(
                    "No collections yet. Create one below!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            collections.forEach { collection ->
                val isAdded = coinCollectionIds.contains(collection.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isAdded) onRemoveFromCollection(collection.id)
                            else onAddToCollection(collection.id)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(collection.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${collection.coins.size} coins",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = isAdded,
                        onCheckedChange = {
                            if (isAdded) onRemoveFromCollection(collection.id)
                            else onAddToCollection(collection.id)
                        }
                    )
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(16.dp))
            if (showCreateDialog) {
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    label = { Text("Collection name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showCreateDialog = false; newCollectionName = "" },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (newCollectionName.isNotBlank()) {
                                onCreateCollection(newCollectionName.trim())
                                newCollectionName = ""; showCreateDialog = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Create") }
                }
            } else {
                OutlinedButton(onClick = { showCreateDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ New Collection")
                }
            }
        }
    }
}

// ─── Loading / Error states ───────────────────────────────────────────────────

@Composable
private fun LoadingFooter() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ErrorFooter(message: String?, onRetry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            message ?: "Something went wrong",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ErrorContent(message: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message ?: "Failed to load coins", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text("Retry") }
    }
}
