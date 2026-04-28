package com.example.cryptopricetracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.cryptopricetracker.data.local.CryptoDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable helper that triggers an immediate redraw of all active
 * CryptoCollectionWidgets. Inject this into any ViewModel or UseCase that
 * mutates a collection (add coin, remove coin, delete collection, etc.) so
 * the home-screen widget always reflects the latest state without waiting for
 * the next WorkManager tick or an app foreground event.
 */
@Singleton
class WidgetRefreshHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: CryptoDatabase
) {
    /**
     * Refreshes every active CryptoCollectionWidget on the home screen.
     * Safe to call from a coroutine on any dispatcher — Glance handles
     * its own threading internally.
     */
    suspend fun refreshAll() {
        try {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(CryptoCollectionWidget::class.java)
            ids.forEach { CryptoCollectionWidget().update(context, it) }
        } catch (_: Exception) {
            // No widgets pinned, or Glance not ready — silently ignore
        }
    }

    /**
     * Syncs Room's widget_id column with actually-alive widgets on the home screen.
     * Also clears DataStore entries for removed widgets.
     */
    suspend fun syncWidgetState() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, CryptoCollectionWidgetReceiver::class.java)
            val activeWidgetIds = appWidgetManager.getAppWidgetIds(provider).toSet()

            val allCollections = db.collectionDao().getAllCollections()
                .firstOrNull() ?: return

            allCollections.forEach { entity ->
                val wId = entity.widgetId
                if (wId != null && wId !in activeWidgetIds) {
                    db.collectionDao().clearWidgetId(wId)
                    WidgetPrefsStore.clearCollectionForWidget(context, wId)
                }
            }
        } catch (_: Exception) {
            // Best-effort sync
        }
    }

    /**
     * Called when a widget is removed from the home screen (from the receiver's onDeleted).
     * Clears DataStore AND Room's widget_id column.
     */
    suspend fun onWidgetRemoved(appWidgetId: Int) {
        WidgetPrefsStore.clearCollectionForWidget(context, appWidgetId)
        db.collectionDao().clearWidgetId(appWidgetId)
    }

    /**
     * Called when a collection is deleted from the app.
     * Clears the widget mapping so the widget shows "No collection" immediately,
     * then refreshes all widgets.
     */
    suspend fun onCollectionDeleted(@Suppress("UNUSED_PARAMETER") collectionId: Long, widgetId: Int?) {
        if (widgetId != null) {
            WidgetPrefsStore.clearCollectionForWidget(context, widgetId)
            db.collectionDao().clearWidgetId(widgetId)
        }
        refreshAll()
    }
}
