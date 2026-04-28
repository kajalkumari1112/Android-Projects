package com.example.cryptopricetracker.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.PendingIntentCompat
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.widget.CryptoCollectionWidgetReceiver
import com.example.cryptopricetracker.widget.WidgetConfigActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinWidgetBottomSheet(
    collections: List<CoinCollection>,
    preselectedCollectionId: Long? = null,
    onSyncWidgetState: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // ✅ FIX 1 & 2: Sync widget state on open — clears stale "Already added" labels
    // if the user removed the widget from the home screen externally.
    LaunchedEffect(Unit) {
        onSyncWidgetState?.invoke()
    }

    // Pre-select collection — only if it's NOT already pinned
    var selectedCollection by remember {
        mutableStateOf(
            collections.firstOrNull { it.id == preselectedCollectionId && !it.isPinnedAsWidget }
        )
    }

    // Re-evaluate selection when collections update (e.g. after sync clears stale flags)
    LaunchedEffect(collections) {
        val current = selectedCollection
        if (current != null) {
            // Refresh from the updated list
            val updated = collections.firstOrNull { it.id == current.id }
            selectedCollection = if (updated?.isPinnedAsWidget == true) null else updated
        } else if (preselectedCollectionId != null) {
            selectedCollection = collections.firstOrNull {
                it.id == preselectedCollectionId && !it.isPinnedAsWidget
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Add Collection Widget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose a collection to display on your home screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // ── Collection picker ────────────────────────────────────────
            if (collections.isEmpty()) {
                Text(
                    "No collections yet. Create one from the coin list first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Check if ALL collections are already pinned
                val allPinned = collections.all { it.isPinnedAsWidget }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(collections, key = { it.id }) { collection ->
                        val isSelected = selectedCollection?.id == collection.id
                        CollectionPickerItem(
                            collection = collection,
                            isSelected = isSelected,
                            onClick = {
                                if (collection.isPinnedAsWidget) {
                                    // ✅ FIX 2: already pinned — show message instead
                                    selectedCollection = null
                                } else {
                                    selectedCollection = collection
                                }
                            },
                            isPinned = collection.isPinnedAsWidget
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Pin button ───────────────────────────────────────────
                Button(
                    onClick = {
                        selectedCollection?.let { col ->
                            if (col.isPinnedAsWidget) {
                                // Should not happen since we guard selection, but just in case
                                return@Button
                            }
                            // ✅ FIX 1: Just pin — no custom dialog.
                            // The OS shows its own confirmation prompt.
                            pinWidgetToHomeScreen(context, col.id)
                            onDismiss()
                        }
                    },
                    enabled = selectedCollection != null && !allPinned,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pin to Home Screen")
                }

                if (allPinned) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "All collections are already on the home screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionPickerItem(
    collection: CoinCollection,
    isSelected: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isPinned -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPinned)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${collection.coins.size} coin${if (collection.coins.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPinned) {
                    Text(
                        text = "Already added to home screen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            when {
                isPinned -> Icon(
                    Icons.Default.Lock,
                    contentDescription = "Already pinned",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                isSelected -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun pinWidgetToHomeScreen(context: Context, collectionId: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetProvider = ComponentName(context, CryptoCollectionWidgetReceiver::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                putExtra(WidgetConfigActivity.EXTRA_PRESELECTED_COLLECTION_ID, collectionId)
            }
            val successCallback = PendingIntentCompat.getActivity(
                context,
                collectionId.toInt(),
                configIntent,
                0,
                true
            )
            val extras = Bundle().apply {
                putLong(WidgetConfigActivity.EXTRA_PRESELECTED_COLLECTION_ID, collectionId)
            }
            appWidgetManager.requestPinAppWidget(widgetProvider, extras, successCallback)
        } else {
            val intent = Intent(context, WidgetConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(WidgetConfigActivity.EXTRA_PRESELECTED_COLLECTION_ID, collectionId)
            }
            context.startActivity(intent)
        }
    } else {
        val intent = Intent(context, WidgetConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(WidgetConfigActivity.EXTRA_PRESELECTED_COLLECTION_ID, collectionId)
        }
        context.startActivity(intent)
    }
}
