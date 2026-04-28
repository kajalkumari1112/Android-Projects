package com.example.cryptopricetracker.presentation.coinlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                    // Quick widget menu — requirement 2c: widget setup within the app
                    IconButton(onClick = { showWidgetSheet = true }) {
                        Icon(Icons.Default.Widgets, contentDescription = "Add Widget")
                    }
                    IconButton(onClick = onNavigateToCollections) {
                        Icon(Icons.Default.FolderCopy, contentDescription = "My Collections")
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
                onCoinVisible = { coinId -> viewModel.observeCollectionMembership(coinId) }
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

@Composable
private fun CoinPagingList(
    pagedCoins: LazyPagingItems<Coin>,
    uiState: CoinListUiState,
    onAddToCollection: (String, Long) -> Unit,
    onRemoveFromCollection: (String, Long) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    onCoinVisible: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            CoinListHeader()
        }

        // Coin items
        items(
            count = pagedCoins.itemCount,
            key = pagedCoins.itemKey { it.id }
        ) { index ->
            val coin = pagedCoins[index] ?: return@items
            // Trigger collection membership observation when item becomes visible
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
                onCreateCollection = onCreateCollection
            )
        }

        // Loading footer
        when (val state = pagedCoins.loadState.append) {
            is LoadState.Loading -> item { LoadingFooter() }
            is LoadState.Error -> item { ErrorFooter(state.error.message) { pagedCoins.retry() } }
            else -> Unit
        }

        // Full-screen error on REFRESH
        when (val state = pagedCoins.loadState.refresh) {
            is LoadState.Error -> item {
                ErrorContent(state.error.message) { pagedCoins.refresh() }
            }
            else -> Unit
        }
    }
}

@Composable
private fun CoinListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Coin",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Price / 24h Change",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}

@Composable
fun CoinListItem(
    coin: Coin,
    livePrice: Double,
    collections: List<CoinCollection>,
    coinCollectionIds: List<Long>,
    onAddToCollection: (String, Long) -> Unit,
    onRemoveFromCollection: (String, Long) -> Unit,
    onCreateCollection: (String, String) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val isInAnyCollection = coinCollectionIds.isNotEmpty()

    val bookmarkTint by animateColorAsState(
        targetValue = if (isInAnyCollection) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "bookmark_tint"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            coin.marketCapRank?.let { rank ->
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp)
                )
            }

            // Coin icon
            AsyncImage(
                model = coin.imageUrl,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )

            Spacer(Modifier.width(12.dp))

            // Name + symbol
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = coin.symbol.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Price + change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(livePrice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                PriceChangeBadge(coin.priceChangePercentage24h)
            }

            Spacer(Modifier.width(8.dp))

            // Bookmark / Add to collection
            IconButton(onClick = { showBottomSheet = true }) {
                Icon(
                    imageVector = if (isInAnyCollection) Icons.Default.Bookmark
                    else Icons.Default.BookmarkBorder,
                    contentDescription = "Add to collection",
                    tint = bookmarkTint
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
            onRemoveFromCollection = { collectionId ->
                onRemoveFromCollection(coin.id, collectionId)
            },
            onCreateCollection = { name ->
                onCreateCollection(coin.id, name)
                showBottomSheet = false
            }
        )
    }
}

@Composable
private fun PriceChangeBadge(changePercent: Double?) {
    if (changePercent == null) return
    val isPositive = changePercent >= 0
    val color = if (isPositive) Color(0xFF00C853) else Color(0xFFD50000)
    val icon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            text = "${"%.2f".format(changePercent)}%",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

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
                    text = "No collections yet. Create one below!",
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
                    Checkbox(checked = isAdded, onCheckedChange = {
                        if (isAdded) onRemoveFromCollection(collection.id)
                        else onAddToCollection(collection.id)
                    })
                }
                HorizontalDivider()
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Create new collection")
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Collection") },
            text = {
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    label = { Text("Collection name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            onCreateCollection(newCollectionName.trim())
                            showCreateDialog = false
                            newCollectionName = ""
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ErrorFooter(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message ?: "An error occurred",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ErrorContent(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Failed to load coins",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message ?: "Unknown error",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}

fun formatPrice(price: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return if (price < 1.0) {
        formatter.maximumFractionDigits = 6
        formatter.format(price)
    } else {
        formatter.format(price)
    }
}
