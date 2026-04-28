package com.example.cryptopricetracker.presentation.collections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Collections", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.collections.isEmpty()) {
            EmptyCollectionsState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.collections, key = { it.id }) { collection ->
                    CollectionCard(
                        collection = collection,
                        onDetailClick = { onCollectionClick(collection.id) },
                        onDelete = {
                            viewModel.onEvent(CollectionsEvent.DeleteCollection(collection.id))
                        },
                        onRemoveCoin = { coinId ->
                            viewModel.onEvent(CollectionsEvent.RemoveCoin(collection.id, coinId))
                        }
                    )
                }
            }
        }
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${collection.coins.size} coin${if (collection.coins.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (collection.isPinnedAsWidget) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = "Pinned as widget",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete collection",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Expanded coin list
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (collection.coins.isEmpty()) {
                    Text(
                        "No coins in this collection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    collection.coins.forEach { coin ->
                        CollectionCoinRow(coin = coin, onRemove = { onRemoveCoin(coin.id) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDetailClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Details")
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
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CollectionCoinRow(coin: Coin, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(coin.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                coin.symbol.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            formatPrice(coin.currentPrice),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onRemove) {
            Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
private fun EmptyCollectionsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No Collections Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap the bookmark icon on any coin to add it to a collection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

