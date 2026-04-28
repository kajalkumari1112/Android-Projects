package com.example.cryptopricetracker.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
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

// ── Deep-link URI that opens Collections in MainActivity ──────────────────────
private const val COLLECTIONS_URI = "crypto://collections"

class CryptoCollectionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CryptoWidgetEntryPoint::class.java
        )
        val db = entryPoint.database()

        val appWidgetId: Int = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) {
            -1
        }

        // One-time promotion of a pending mapping (safe if already promoted)
        if (appWidgetId != -1) {
            promotePendingMapping(context, db, appWidgetId)
        }

        provideContent {
            val collectionId by produceState<Long?>(initialValue = null, appWidgetId) {
                if (appWidgetId == -1) { value = null; return@produceState }
                WidgetPrefsStore.collectionIdFlow(context, appWidgetId).collect { storedId ->
                    value = storedId
                        ?: db.collectionDao().getCollectionByWidgetId(appWidgetId)?.id
                        ?: db.collectionDao().getAllCollectionsWithCoins()
                            .firstOrNull()?.firstOrNull()?.collection?.id
                }
            }

            val collection by produceState<CoinCollection?>(initialValue = null, collectionId) {
                val cid = collectionId ?: run { value = null; return@produceState }
                db.collectionDao()
                    .getCollectionWithCoins(cid)
                    .collect { relation -> value = relation?.toDomain() }
            }
            WidgetContent(collectionId = collectionId, collection = collection)
        }
    }

    private suspend fun promotePendingMapping(
        context: Context, db: CryptoDatabase, appWidgetId: Int
    ) {
        // Only promote if this widget doesn't already have a mapping
        if (WidgetPrefsStore.containsWidget(context, appWidgetId)) return
        val pendingId = WidgetPrefsStore.getPendingCollectionId(context) ?: return
        WidgetPrefsStore.promotePending(context, appWidgetId, pendingId)
        db.collectionDao().clearWidgetId(appWidgetId)
        db.collectionDao().setWidgetId(pendingId, appWidgetId)
    }
}

// ─── Palette ─────────────────────────────────────────────────────────────────
// ColorProvider(day, night) is the correct public Glance API for static colors.

private val BgDark        = ColorProvider(day = Color(0xFF0D1117), night = Color(0xFF0D1117))
private val AccentGold    = ColorProvider(day = Color(0xFFF0B429), night = Color(0xFFF0B429))
private val TextPrimary   = ColorProvider(day = Color(0xFFE6EDF3), night = Color(0xFFE6EDF3))
private val TextSecondary = ColorProvider(day = Color(0xFF8B949E), night = Color(0xFF8B949E))
private val GreenColor    = ColorProvider(day = Color(0xFF3FB950), night = Color(0xFF3FB950))
private val RedColor      = ColorProvider(day = Color(0xFFF85149), night = Color(0xFFF85149))
private val DividerColor  = ColorProvider(day = Color(0xFF21262D), night = Color(0xFF21262D))
private val GreenBg       = ColorProvider(day = Color(0xFF1A3B2A), night = Color(0xFF1A3B2A))
private val RedBg         = ColorProvider(day = Color(0xFF3B1A1A), night = Color(0xFF3B1A1A))
private val LiveBadgeBg   = ColorProvider(day = Color(0xFF1A3B2A), night = Color(0xFF1A3B2A))

// ─── Widget root ─────────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(collectionId: Long?, collection: CoinCollection?) {
    // Build deep-link: crypto://collections/{collectionId} → lands on CollectionDetail
    // Falls back to crypto://collections (Collections list) if ID not yet resolved
    val uri = if (collectionId != null)
        Uri.parse("$COLLECTIONS_URI/$collectionId")
    else
        Uri.parse(COLLECTIONS_URI)

    val openIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val tapAction = actionStartActivity(openIntent)

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BgDark)
                .appWidgetBackground()
                .cornerRadius(20.dp)
        ) {
            if (collection == null) {
                EmptyWidget(tapAction)
            } else {
                CollectionWidget(collection, tapAction)
            }
        }
    }
}

// ─── Empty / unconfigured state ───────────────────────────────────────────────

@Composable
private fun EmptyWidget(tapAction: androidx.glance.action.Action) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(20.dp)
            .clickable(tapAction),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "₿",
            style = TextStyle(color = AccentGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = "No collection selected",
            style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Tap to open the app",
            style = TextStyle(color = TextSecondary, fontSize = 11.sp)
        )
    }
}

// ─── Populated widget ────────────��───────────────────────────────────────────

@Composable
private fun CollectionWidget(collection: CoinCollection, tapAction: androidx.glance.action.Action) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 10.dp)
    ) {
        // Header is tappable so the top area also opens the app
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(end = 12.dp)
                .clickable(tapAction),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gold accent bar
            Box(
                modifier = GlanceModifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(AccentGold)
                    .cornerRadius(2.dp)
            ) {}
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = collection.name.uppercase(),
                style = TextStyle(
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(GlanceModifier.width(6.dp))
            // "LIVE" pill badge
            Box(
                modifier = GlanceModifier
                    .background(LiveBadgeBg)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "● LIVE",
                    style = TextStyle(color = GreenColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(GlanceModifier.height(10.dp))

        // ── Divider ───────��──────────────────────────────────────────────────
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        ) {}

        Spacer(GlanceModifier.height(6.dp))

        // ── Coin rows ────────────────────────────────────────────────────────
        if (collection.coins.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No coins in this collection",
                    style = TextStyle(color = TextSecondary, fontSize = 11.sp)
                )
            }
        } else {
            if (collection.coins.size <= 3) {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    collection.coins.forEach { coin -> CoinRow(coin, tapAction) }
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(114.dp)
                ) {
                    items(collection.coins) { coin -> CoinRow(coin, tapAction) }
                }
            }
        }
    }
}

// ─── Single coin row ─────────────────────────────────────────────────────────

@Composable
private fun CoinRow(coin: Coin, tapAction: androidx.glance.action.Action) {
    val change = coin.priceChangePercentage24h ?: 0.0
    val isPositive = change >= 0
    val changeColor = if (isPositive) GreenColor else RedColor
    val changeSign  = if (isPositive) "▲" else "▼"
    val changeBg    = if (isPositive) GreenBg else RedBg

    // Wrap in a Column so both the row content and the divider are clickable
    Column(
        modifier = GlanceModifier.fillMaxWidth().clickable(tapAction)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                // 12dp end-padding keeps price/badge clear of the OS scrollbar track
                .padding(top = 5.dp, bottom = 5.dp, start = 0.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol + name column
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = coin.symbol.uppercase(),
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = coin.name,
                    style = TextStyle(color = TextSecondary, fontSize = 10.sp),
                    maxLines = 1
                )
            }

            // Price + change pill column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(coin.currentPrice),
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(GlanceModifier.height(2.dp))
                Box(
                    modifier = GlanceModifier
                        .background(changeBg)
                        .cornerRadius(6.dp)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$changeSign ${"%.2f".format(kotlin.math.abs(change))}%",
                        style = TextStyle(color = changeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        ) {}
    }
}

private fun formatPrice(price: Double): String = when {
    price >= 1_000 -> "\$${"%.0f".format(price)}"
    price >= 1     -> "\$${"%.2f".format(price)}"
    else           -> "\$${"%.4f".format(price)}"
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
            appWidgetIds.forEach { id ->
                WidgetPrefsStore.clearCollectionForWidget(context, id)
                db.collectionDao().clearWidgetId(id)
            }
        }
    }
}
