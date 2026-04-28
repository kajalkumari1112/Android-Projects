package com.example.cryptopricetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.cryptopricetracker.data.local.CryptoDatabase
import com.example.cryptopricetracker.data.mapper.toDomain
import com.example.cryptopricetracker.domain.model.Coin
import com.example.cryptopricetracker.domain.model.CoinCollection
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.firstOrNull

/**
 * Jetpack Glance widget that displays coins from a user-selected collection.
 *
 * ── Real-time updates ──────────────────────────────────────────────────────
 * The Room Flow is observed INSIDE provideContent via produceState.
 * provideContent's lambda is a live composable scope that stays alive for the
 * widget's lifetime, so any DB write (add/remove coin) automatically triggers
 * a recomposition — no explicit update() call is needed from the app side.
 *
 * ── Collection resolution order (most → least reliable) ───────────────────
 *  1. DataStore keyed by appWidgetId      ← set immediately on config
 *  2. Room DB widget_id column            ← secondary store
 *  3. "Pending" DataStore key             ← when ID unknown at config time
 *  4. First collection in DB             ← last-resort, never blank
 */
class CryptoCollectionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CryptoWidgetEntryPoint::class.java
        )
        val db = entryPoint.database()

        val appWidgetId: Int? = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) { null }

        if (appWidgetId != null) {
            promotePendingMapping(context, db, appWidgetId)
        }

        // Resolve the stable collection ID once — this becomes the key for the live Flow below
        val collectionId = resolveCollectionId(context, db, appWidgetId)

        provideContent {
            val collection by produceState<CoinCollection?>(
                initialValue = null,
                key1 = collectionId
            ) {
                if (collectionId == null) {
                    value = null
                    return@produceState
                }
                db.collectionDao()
                    .getCollectionWithCoins(collectionId)
                    .collect { relation -> value = relation?.toDomain() }
            }

            WidgetContent(collection = collection)
        }
    }

    // ── Collection ID resolution (run once before provideContent) ─────────────

    private suspend fun resolveCollectionId(
        context: Context,
        db: CryptoDatabase,
        appWidgetId: Int?
    ): Long? {
        if (appWidgetId == null) return null

        // 1) DataStore by real widgetId
        WidgetPrefsStore.getCollectionIdForWidget(context, appWidgetId)?.let { return it }

        // 2) Room DB widget_id column
        db.collectionDao().getCollectionByWidgetId(appWidgetId)?.id?.let { return it }

        // 3) Pending mapping
        WidgetPrefsStore.getPendingCollectionId(context)?.let { return it }

        // 4) First collection in DB
        return db.collectionDao().getAllCollectionsWithCoins()
            .firstOrNull()?.firstOrNull()?.collection?.id
    }

    private suspend fun promotePendingMapping(
        context: Context,
        db: CryptoDatabase,
        appWidgetId: Int
    ) {
        val pendingId = WidgetPrefsStore.getPendingCollectionId(context) ?: return
        if (WidgetPrefsStore.containsWidget(context, appWidgetId)) return

        WidgetPrefsStore.promotePending(context, appWidgetId, pendingId)
        db.collectionDao().clearWidgetId(appWidgetId)
        db.collectionDao().setWidgetId(pendingId, appWidgetId)
    }
}

// ─── Widget UI ────────────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(collection: CoinCollection?) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
                .appWidgetBackground()
                .cornerRadius(16.dp)
        ) {
            if (collection == null) {
                NoCollectionContent()
            } else {
                CollectionContent(collection)
            }
        }
    }
}

@Composable
private fun CollectionContent(collection: CoinCollection) {
    // Fixed header
    Text(
        text = collection.name,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1,
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    )

    val coins = collection.coins

    if (coins.isEmpty()) {
        Text(
            text = "No coins in this collection.",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            ),
            modifier = GlanceModifier.padding(top = 4.dp)
        )
    } else {
        // Always use LazyColumn — it scrolls automatically if content exceeds the
        // available cell height, and wraps naturally when it fits. No need to
        // distinguish between ≤5 and >5 items.
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxWidth()
        ) {
            items(coins) { coin -> CoinWidgetRow(coin = coin) }
        }
    }
}

@Composable
private fun CoinWidgetRow(coin: Coin) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 0.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Symbol
        Text(
            text = coin.symbol.uppercase(),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        // Price + change
        val isPositive = (coin.priceChangePercentage24h ?: 0.0) >= 0
        val changeColor = if (isPositive) GlanceTheme.colors.primary else GlanceTheme.colors.error

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "\$${"%.2f".format(coin.currentPrice)}",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp)
            )
            coin.priceChangePercentage24h?.let { change ->
                Text(
                    text = "${"%.2f".format(change)}%",
                    style = TextStyle(color = changeColor, fontSize = 10.sp)
                )
            }
        }
    }
}

@Composable
private fun NoCollectionContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No collection selected",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Tap to configure",
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp)
        )
    }
}

// ─── Receiver ────────────────────────────────────────────────────────────────

class CryptoCollectionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CryptoCollectionWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CryptoWidgetEntryPoint::class.java
        )
        val db = entryPoint.database()
        kotlinx.coroutines.runBlocking {
            appWidgetIds.forEach { widgetId ->
                WidgetPrefsStore.clearCollectionForWidget(context, widgetId)
                db.collectionDao().clearWidgetId(widgetId)
            }
        }
    }
}
