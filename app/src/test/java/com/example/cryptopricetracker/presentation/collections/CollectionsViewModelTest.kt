package com.example.cryptopricetracker.presentation.collections

import app.cash.turbine.test
import com.example.cryptopricetracker.domain.model.CoinCollection
import com.example.cryptopricetracker.domain.usecase.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val getAllCollections = mockk<GetAllCollectionsUseCase>()
    private val deleteCollection = mockk<DeleteCollectionUseCase>(relaxed = true)
    private val removeCoinFromCollection = mockk<RemoveCoinFromCollectionUseCase>(relaxed = true)
    private val pinCollectionAsWidget = mockk<PinCollectionAsWidgetUseCase>(relaxed = true)

    private lateinit var viewModel: CollectionsViewModel

    private val fakeCollections = listOf(
        CoinCollection(id = 1L, name = "Altcoins", createdAt = 0L, widgetId = null, coins = emptyList()),
        CoinCollection(id = 2L, name = "DeFi", createdAt = 0L, widgetId = 5, coins = emptyList())
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getAllCollections() } returns flowOf(fakeCollections)
        viewModel = CollectionsViewModel(
            getAllCollections,
            deleteCollection,
            removeCoinFromCollection,
            pinCollectionAsWidget
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads collections into uiState`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.collections.size)
            assertEquals("Altcoins", state.collections[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DeleteCollection event calls deleteCollection use case`() = runTest {
        viewModel.onEvent(CollectionsEvent.DeleteCollection(collectionId = 1L))
        coVerify(exactly = 1) { deleteCollection(1L) }
    }

    @Test
    fun `DeleteCollection event emits effect message`() = runTest {
        viewModel.effect.test {
            viewModel.onEvent(CollectionsEvent.DeleteCollection(collectionId = 1L))
            assertEquals("Collection deleted.", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RemoveCoin event calls removeCoinFromCollection use case`() = runTest {
        viewModel.onEvent(CollectionsEvent.RemoveCoin(collectionId = 1L, coinId = "bitcoin"))
        coVerify(exactly = 1) { removeCoinFromCollection(1L, "bitcoin") }
    }

    @Test
    fun `PinAsWidget event calls pinCollectionAsWidget use case`() = runTest {
        viewModel.onEvent(CollectionsEvent.PinAsWidget(collectionId = 2L, widgetId = 10))
        coVerify(exactly = 1) { pinCollectionAsWidget(2L, 10) }
    }

    @Test
    fun `second collection is already pinned as widget`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.collections[1].isPinnedAsWidget)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

