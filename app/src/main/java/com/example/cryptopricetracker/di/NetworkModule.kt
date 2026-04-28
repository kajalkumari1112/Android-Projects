package com.example.cryptopricetracker.di

import com.example.cryptopricetracker.data.remote.api.CoinGeckoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3/"

    /**
     * Rate-limit interceptor: CoinGecko free tier allows ~10-30 req/min.
     * If we get HTTP 429 (Too Many Requests), wait and retry up to 3 times
     * with exponential backoff. This prevents the "HTTP 429 retry" error
     * from surfacing to the user during fast scrolling.
     */
    private val rateLimitInterceptor = Interceptor { chain ->
        var request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0
        val maxRetries = 3

        while (response.code == 429 && retryCount < maxRetries) {
            response.close()
            // Exponential backoff: 2s, 4s, 8s
            val waitMs = (2000L * (1 shl retryCount))
            Thread.sleep(waitMs)
            retryCount++
            response = chain.proceed(request)
        }

        response
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(rateLimitInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC  // BASIC instead of BODY — less overhead
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(COINGECKO_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideCoinGeckoApiService(retrofit: Retrofit): CoinGeckoApiService =
        retrofit.create(CoinGeckoApiService::class.java)
}
