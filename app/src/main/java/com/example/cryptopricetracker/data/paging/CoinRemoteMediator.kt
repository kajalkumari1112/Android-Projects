package com.example.cryptopricetracker.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.cryptopricetracker.data.local.CryptoDatabase
import com.example.cryptopricetracker.data.local.entity.CoinEntity
import com.example.cryptopricetracker.data.local.entity.RemoteKeyEntity
import com.example.cryptopricetracker.data.mapper.toEntity
import com.example.cryptopricetracker.data.remote.api.CoinGeckoApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val STARTING_PAGE = 1
private const val PAGE_SIZE = 50

@OptIn(ExperimentalPagingApi::class)
class CoinRemoteMediator @Inject constructor(
    private val api: CoinGeckoApiService,
    private val db: CryptoDatabase
) : RemoteMediator<Int, CoinEntity>() {

    private val coinDao = db.coinDao()
    private val remoteKeyDao = db.remoteKeyDao()

    override suspend fun initialize(): InitializeAction {
        // Only refresh if cache is empty; otherwise serve from Room immediately
        return if (coinDao.count() > 0) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CoinEntity>
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> STARTING_PAGE
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    // Get the remote key of the last item loaded
                    val lastItem = state.lastItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = false)
                    val remoteKey = remoteKeyDao.remoteKeyByCoinId(lastItem.id)
                    remoteKey?.nextPage
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            val coins = api.getCoins(page = page, perPage = PAGE_SIZE)
            val endOfPagination = coins.isEmpty() || coins.size < PAGE_SIZE

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    coinDao.clearAll()
                    remoteKeyDao.clearAll()
                }

                val nextPage = if (endOfPagination) null else page + 1
                val prevPage = if (page == STARTING_PAGE) null else page - 1
                val offset = (page - 1) * PAGE_SIZE

                val remoteKeys = coins.map { RemoteKeyEntity(it.id, prevPage, nextPage) }
                val entities = coins.mapIndexed { index, dto ->
                    dto.toEntity(pageNumber = page, position = offset + index)
                }

                remoteKeyDao.insertAll(remoteKeys)
                coinDao.insertAll(entities)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPagination)

        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            if (e.code() == 429) {
                // Rate limited — don't propagate as error, just signal end for now.
                // The OkHttp interceptor already retried 3 times with backoff.
                // Returning Success(false) lets the user scroll cached data and
                // paging will retry automatically on the next scroll.
                MediatorResult.Success(endOfPaginationReached = false)
            } else {
                MediatorResult.Error(e)
            }
        }
    }
}
