package com.example.cryptopricetracker.di

import android.content.Context
import androidx.room.Room
import com.example.cryptopricetracker.data.local.CryptoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CryptoDatabase =
        Room.databaseBuilder(context, CryptoDatabase::class.java, "crypto_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}
