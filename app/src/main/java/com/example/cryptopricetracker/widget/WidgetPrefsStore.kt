package com.example.cryptopricetracker.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed storage for widget ↔ collection mappings.
 * Replaces the old SharedPreferences approach with Jetpack DataStore.
 *
 * Keys:
 *  - "collection_id_{widgetId}" → the collection ID assigned to that widget
 *  - "collection_id_pending"    → fallback when the real widgetId is unknown at config time
 */
private val Context.widgetDataStore by preferencesDataStore(name = "crypto_widget_prefs")

object WidgetPrefsStore {

    private const val PENDING_KEY_NAME = "collection_id_pending"
    private val PENDING_KEY = longPreferencesKey(PENDING_KEY_NAME)

    private fun widgetKey(appWidgetId: Int) = longPreferencesKey("collection_id_$appWidgetId")

    // ── Reads ────────────────────────────────────────────────────────────────

    suspend fun getCollectionIdForWidget(context: Context, appWidgetId: Int): Long? {
        val key = widgetKey(appWidgetId)
        return context.widgetDataStore.data
            .map { prefs -> prefs[key] }
            .firstOrNull()
    }

    suspend fun containsWidget(context: Context, appWidgetId: Int): Boolean {
        val key = widgetKey(appWidgetId)
        return context.widgetDataStore.data
            .map { prefs -> prefs.contains(key) }
            .firstOrNull() ?: false
    }

    suspend fun getPendingCollectionId(context: Context): Long? {
        return context.widgetDataStore.data
            .map { prefs -> prefs[PENDING_KEY] }
            .firstOrNull()
    }

    // ── Writes ───────────────────────────────────────────────────────────────

    suspend fun saveCollectionForWidget(context: Context, appWidgetId: Int, collectionId: Long) {
        context.widgetDataStore.edit { prefs ->
            prefs[widgetKey(appWidgetId)] = collectionId
        }
    }

    suspend fun savePendingCollection(context: Context, collectionId: Long) {
        context.widgetDataStore.edit { prefs ->
            prefs[PENDING_KEY] = collectionId
        }
    }

    suspend fun clearCollectionForWidget(context: Context, appWidgetId: Int) {
        context.widgetDataStore.edit { prefs ->
            prefs.remove(widgetKey(appWidgetId))
        }
    }

    /**
     * Promote a pending mapping to a real per-widget entry and clear the pending key.
     */
    suspend fun promotePending(context: Context, appWidgetId: Int, pendingCollectionId: Long) {
        context.widgetDataStore.edit { prefs ->
            prefs[widgetKey(appWidgetId)] = pendingCollectionId
            prefs.remove(PENDING_KEY)
        }
    }

    suspend fun clearPending(context: Context) {
        context.widgetDataStore.edit { prefs ->
            prefs.remove(PENDING_KEY)
        }
    }
}

