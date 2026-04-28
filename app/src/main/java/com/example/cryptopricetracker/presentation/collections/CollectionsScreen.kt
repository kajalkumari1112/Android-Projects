package com.example.cryptopricetracker.presentation.collections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
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
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.presentation.coinlist.formatPrice
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    onNavigateBack: () -> Unit,
    onCollectionClick: (Long) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    val totalCoins = uiState.collections.sumOf { it.coins.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Collections", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${uiState.collections.size} collection${if (uiState.collections.size != 1) "s" else ""} · $totalCoins coin${if (totalCoins != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            .background(Color(0xFFD6CCFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New Collection", tint = Color(0xFF6C3CE1))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.collections.isEmpty()) {
            EmptyCollectionsState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.collections, key = { it.id }) { collection ->
                    CollectionCard(
                        collection = collection,
                        onDetailClick = { onCollectionClick(collection.id) },
                        onDelete = { viewModel.onEvent(CollectionsEvent.DeleteCollection(collection.id)) },
                        onRemoveCoin = { coinId -> viewModel.onEvent(CollectionsEvent.RemoveCoin(collection.id, coinId)) }
                    )
                }
            }
        }
    }

    // Create collection dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newCollectionName = "" },
            title = { Text("New Collection") },
            text = {
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    label = { Text("Collection name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            viewModel.onEvent(CollectionsEvent.CreateCollection(newCollectionName.trim()))
                            newCollectionName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C3CE1))
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newCollectionName = "" }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CollectionCard(
    collection: CoinCollection,
    onDetailClick: () -> Unit,
    onDelete: () -> Unit,
    onRemoveCoin: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name + count + coin avatar circles
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-6).dp)
                    ) {
                        Text(
                            text = "${collection.coins.size} coin${if (collection.coins.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        collection.coins.take(4).forEachIndexed { idx, coin ->
                            CoinAvatarCircle(
                                symbol = coin.symbol,
                                colorIndex = idx,
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Grid icon button (purple)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD6CCFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GridView, contentDescription = "Grid view", tint = Color(0xFF6C3CE1), modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(8.dp))

                // Expand/Collapse button (blue border when expanded)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (expanded) Modifier.border(2.dp, Color(0xFF2979FF), RoundedCornerShape(12.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = if (expanded) Color(0xFF2979FF) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Delete button (light red background)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFE5E5)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── Expanded coin list ───────────────────────────────────────────
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                if (collection.coins.isEmpty()) {
                    Text(
                        "No coins in this collection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    collection.coins.forEachIndexed { idx, coin ->
                        ExpandedCoinRow(
                            coin = coin,
                            colorIndex = idx,
                            onRemove = { onRemoveCoin(coin.id) }
                        )
                        if (idx < collection.coins.lastIndex) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // View Details button — light purple fill
                Button(
                    onClick = onDetailClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8E3FF),
                        contentColor = Color(0xFF6C3CE1)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("View Details", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Collection") },
            text = { Text("Are you sure you want to delete '${collection.name}'?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CoinAvatarCircle(symbol: String, colorIndex: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(avatarColor(colorIndex)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.take(2).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            fontSize = 7.sp
        )
    }
}

@Composable
private fun ExpandedCoinRow(coin: Coin, colorIndex: Int, onRemove: () -> Unit) {
    val isPositive = (coin.priceChangePercentage24h ?: 0.0) >= 0
    val changeColor = if (isPositive) Color(0xFF00C853) else Color(0xFFD50000)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coin avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor(colorIndex)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = coin.symbol.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        Spacer(Modifier.width(10.dp))

        // Name + symbol
        Column(modifier = Modifier.weight(1f)) {
            Text(coin.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(coin.symbol.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Mini sparkline
        MiniSparkline(isPositive = isPositive, color = changeColor, modifier = Modifier.size(width = 50.dp, height = 28.dp))

        Spacer(Modifier.width(10.dp))

        // Price + change
        Column(horizontalAlignment = Alignment.End) {
            Text(formatPrice(coin.currentPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            val changeStr = coin.priceChangePercentage24h?.let {
                "${if (it >= 0) "▲" else "▼"} ${"%.2f".format(Math.abs(it))}%"
            } ?: ""
            Text(changeStr, style = MaterialTheme.typography.labelSmall, color = changeColor, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.width(4.dp))

        // Remove (trash)
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove coin", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MiniSparkline(isPositive: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val xs = listOf(0f, w * 0.25f, w * 0.5f, w * 0.75f, w)
        val ys: List<Float> = if (isPositive)
            listOf(h * .75f, h * .5f, h * .6f, h * .3f, h * .1f)
        else
            listOf(h * .25f, h * .5f, h * .4f, h * .7f, h * .9f)
        val fill = Path().apply {
            moveTo(xs[0], ys[0]); for (i in 1 until xs.size) lineTo(xs[i], ys[i])
            lineTo(xs.last(), h); lineTo(xs.first(), h); close()
        }
        drawPath(fill, color = color.copy(alpha = 0.12f))
        val line = Path().apply { moveTo(xs[0], ys[0]); for (i in 1 until xs.size) lineTo(xs[i], ys[i]) }
        drawPath(line, color = color, style = Stroke(width = 2f))
    }
}

@Composable
private fun EmptyCollectionsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No Collections Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Tap the + button to create your first collection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
