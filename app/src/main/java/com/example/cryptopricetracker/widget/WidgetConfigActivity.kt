package com.example.cryptopricetracker.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.cryptopricetracker.data.local.CryptoDatabase
import com.example.cryptopricetracker.data.mapper.toDomain
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.ui.theme.CryptoPriceTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    companion object {
        /** Pass this extra to pre-select a collection (used by in-app pin flow). */
        const val EXTRA_PRESELECTED_COLLECTION_ID = "extra_preselected_collection_id"
    }

    @Inject
    lateinit var db: CryptoDatabase

    @Inject
    lateinit var widgetRefreshHelper: WidgetRefreshHelper

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default result = CANCELED so if user backs out, no widget is added
        setResult(RESULT_CANCELED)

        // Try all known extras for the widget ID — different launchers use different keys
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetId = intent?.extras?.getInt(
                "appWidgetId",
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val preselectedCollectionId = intent?.getLongExtra(EXTRA_PRESELECTED_COLLECTION_ID, -1L)
            ?.takeIf { it != -1L }

        // ✅ FIX #3: If a collection is pre-selected (from in-app pin flow),
        // handle it silently in a coroutine without showing any UI — prevents flicker.
        if (preselectedCollectionId != null) {
            lifecycleScope.launch {
                widgetRefreshHelper.syncWidgetState()
                val raw = db.collectionDao().getAllCollectionsWithCoins().firstOrNull()
                val collections = raw?.map { it.toDomain() } ?: emptyList()
                val target = collections.firstOrNull {
                    it.id == preselectedCollectionId && !it.isPinnedAsWidget
                }
                if (target != null) {
                    saveAndFinish(target)
                } else {
                    // Pre-selected collection is already pinned or not found — show picker UI
                    showConfigUi(collections, preselectedCollectionId = null)
                }
            }
            return
        }

        // No pre-selection — show the picker UI normally
        lifecycleScope.launch {
            widgetRefreshHelper.syncWidgetState()
            val raw = db.collectionDao().getAllCollectionsWithCoins().firstOrNull()
            val collections = raw?.map { it.toDomain() } ?: emptyList()
            showConfigUi(collections, preselectedCollectionId = null)
        }
    }

    private fun showConfigUi(collections: List<CoinCollection>, preselectedCollectionId: Long?) {
        setContent {
            CryptoPriceTrackerTheme {
                var collectionState by remember { mutableStateOf(collections) }

                WidgetConfigScreen(
                    collections = collectionState,
                    preselectedCollectionId = preselectedCollectionId,
                    onCollectionSelected = { collection ->
                        lifecycleScope.launch { saveAndFinish(collection) }
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private suspend fun saveAndFinish(collection: CoinCollection) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetPrefsStore.savePendingCollection(
                this@WidgetConfigActivity, collection.id
            )
        } else {
            db.collectionDao().clearWidgetId(appWidgetId)
            db.collectionDao().setWidgetId(collection.id, appWidgetId)
            WidgetPrefsStore.saveCollectionForWidget(
                this@WidgetConfigActivity, appWidgetId, collection.id
            )
        }

        val glanceManager = GlanceAppWidgetManager(this@WidgetConfigActivity)
        glanceManager.getGlanceIds(CryptoCollectionWidget::class.java)
            .forEach { CryptoCollectionWidget().update(this@WidgetConfigActivity, it) }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    collections: List<CoinCollection>,
    preselectedCollectionId: Long?,
    onCollectionSelected: (CoinCollection) -> Unit,
    onDismiss: () -> Unit
) {
    // Auto-select if pre-specified AND not already pinned
    LaunchedEffect(collections, preselectedCollectionId) {
        if (preselectedCollectionId != null) {
            collections.firstOrNull { it.id == preselectedCollectionId && !it.isPinnedAsWidget }
                ?.let { onCollectionSelected(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Choose a Collection", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (collections.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No collections found.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create a collection in the app first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onDismiss) { Text("Close") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(collections, key = { it.id }) { collection ->
                    val isPinned = collection.isPinnedAsWidget

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isPinned) { onCollectionSelected(collection) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = if (isPinned)
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        else CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    collection.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPinned)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${collection.coins.size} coins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isPinned) {
                                    Text(
                                        "Widget already exists on home screen",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (isPinned) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Already pinned",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
