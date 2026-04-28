package com.example.cryptopricetracker.presentation.collections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.domain.usecase.GetCollectionWithCoinsUseCase
import com.example.cryptopricetracker.presentation.coinlist.formatPrice
import com.example.cryptopricetracker.presentation.widget.PinWidgetBottomSheet
import com.example.cryptopricetracker.presentation.widget.pinWidgetToHomeScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCollectionWithCoins: GetCollectionWithCoinsUseCase
) : androidx.lifecycle.ViewModel() {

    private val collectionId: Long = checkNotNull(savedStateHandle["collectionId"])

    val collection: StateFlow<CoinCollection?> = getCollectionWithCoins(collectionId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: CollectionDetailViewModel = hiltViewModel()
) {
    val collection by viewModel.collection.collectAsState()
    var showWidgetSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collection?.name ?: "Collection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // "Pin as Widget" quick action — requirement 2c: widget setup within the app
                    IconButton(onClick = { showWidgetSheet = true }) {
                        Icon(
                            Icons.Default.Widgets,
                            contentDescription = "Pin as widget",
                            tint = if (collection?.isPinnedAsWidget == true)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        collection?.let { col ->
            if (col.coins.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No coins in this collection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        CollectionSummaryHeader(collection = col)
                    }
                    items(col.coins, key = { it.id }) { coin ->
                        DetailCoinCard(coin = coin)
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    // In-app widget sheet pre-selecting THIS collection
    if (showWidgetSheet) {
        collection?.let { col ->
            PinWidgetBottomSheet(
                collections = listOf(col),
                preselectedCollectionId = col.id,
                onSyncWidgetState = null, // Single-collection view, no VM-level sync needed
                onDismiss = { showWidgetSheet = false }
            )
        }
    }
}

@Composable
private fun CollectionSummaryHeader(collection: CoinCollection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${collection.coins.size} coins tracked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            if (collection.isPinnedAsWidget) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Pinned as home screen widget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailCoinCard(coin: Coin) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coin.imageUrl,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(coin.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    coin.symbol.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatPrice(coin.currentPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                coin.priceChangePercentage24h?.let { change ->
                    val color = if (change >= 0) androidx.compose.ui.graphics.Color(0xFF00C853)
                    else androidx.compose.ui.graphics.Color(0xFFD50000)
                    Text(
                        "${"%.2f".format(change)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
        }
    }
}
