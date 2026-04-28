package com.example.cryptopricetracker.presentation.collections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.domain.usecase.GetCollectionWithCoinsUseCase
import com.example.cryptopricetracker.presentation.coinlist.formatPrice
import com.example.cryptopricetracker.presentation.widget.PinWidgetBottomSheet
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
                title = {
                    Column {
                        Text(
                            collection?.name ?: "Collection",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        collection?.let {
                            Text(
                                "${it.coins.size} coin${if (it.coins.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (collection?.isPinnedAsWidget == true) Color(0xFFD6CCFF)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { showWidgetSheet = true }) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pin as widget",
                                tint = if (collection?.isPinnedAsWidget == true) Color(0xFF6C3CE1)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        collection?.let { col ->
            if (col.coins.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No coins yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Add coins from the home screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary card
                    item { DetailSummaryCard(collection = col) }

                    // Coin cards
                    items(col.coins, key = { it.id }) { coin ->
                        DetailCoinCard(coin = coin, colorIndex = col.coins.indexOf(coin))
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF6C3CE1))
        }
    }

    if (showWidgetSheet) {
        collection?.let { col ->
            PinWidgetBottomSheet(
                collections = listOf(col),
                preselectedCollectionId = col.id,
                onSyncWidgetState = null,
                onDismiss = { showWidgetSheet = false }
            )
        }
    }
}

// ─── Summary card ─────────────────────────────────────────────────────────────

@Composable
private fun DetailSummaryCard(collection: CoinCollection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E3FF)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6C3CE1)
                )
                Text(
                    "${collection.coins.size} coin${if (collection.coins.size != 1) "s" else ""} tracked",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6C3CE1).copy(alpha = 0.7f)
                )
            }
            // Overlapping coin avatar circles
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                collection.coins.take(5).forEachIndexed { idx, coin ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(avatarColor(idx)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            coin.symbol.take(2).uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
        }
    }
}

// ─── Coin detail card ─────────────────────────────────────────────────────────

@Composable
private fun DetailCoinCard(coin: Coin, colorIndex: Int) {
    val isPositive = (coin.priceChangePercentage24h ?: 0.0) >= 0
    val changeColor = if (isPositive) Color(0xFF00C853) else Color(0xFFD50000)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coin avatar circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor(colorIndex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    coin.symbol.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name + symbol
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    coin.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    coin.symbol.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sparkline
            DetailSparkline(
                isPositive = isPositive,
                color = changeColor,
                modifier = Modifier.size(width = 56.dp, height = 32.dp)
            )

            Spacer(Modifier.width(12.dp))

            // Price + change badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatPrice(coin.currentPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                coin.priceChangePercentage24h?.let { change ->
                    Box(
                        modifier = Modifier
                            .background(changeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${if (isPositive) "▲" else "▼"} ${"%.2f".format(Math.abs(change))}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = changeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─── Sparkline ────────────────────────────────────────────────────────────────

@Composable
private fun DetailSparkline(isPositive: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val xs = listOf(0f, w * 0.25f, w * 0.5f, w * 0.75f, w)
        val ys: List<Float> = if (isPositive)
            listOf(h * .75f, h * .55f, h * .65f, h * .35f, h * .15f)
        else
            listOf(h * .25f, h * .45f, h * .35f, h * .65f, h * .85f)
        val fill = Path().apply {
            moveTo(xs[0], ys[0]); for (i in 1 until xs.size) lineTo(xs[i], ys[i])
            lineTo(xs.last(), h); lineTo(xs.first(), h); close()
        }
        drawPath(fill, color = color.copy(alpha = 0.12f))
        val line = Path().apply { moveTo(xs[0], ys[0]); for (i in 1 until xs.size) lineTo(xs[i], ys[i]) }
        drawPath(line, color = color, style = Stroke(width = 2.5f))
    }
}
