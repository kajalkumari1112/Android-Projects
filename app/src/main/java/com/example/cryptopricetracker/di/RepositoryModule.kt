package com.example.cryptopricetracker.di

import com.example.cryptopricetracker.data.repository.CoinRepositoryImpl
import com.example.cryptopricetracker.data.repository.CollectionRepositoryImpl
import com.example.cryptopricetracker.domain.repository.CoinRepository
import com.example.cryptopricetracker.domain.repository.CollectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCoinRepository(impl: CoinRepositoryImpl): CoinRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository
}

