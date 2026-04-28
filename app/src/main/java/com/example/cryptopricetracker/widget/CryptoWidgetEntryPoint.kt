package com.example.cryptopricetracker.widget

import com.example.cryptopricetracker.data.local.CryptoDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for manually injecting dependencies into GlanceAppWidget,
 * which cannot use constructor injection or field injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CryptoWidgetEntryPoint {
    fun database(): CryptoDatabase
}

