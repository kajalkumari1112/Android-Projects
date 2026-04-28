package com.example.cryptopricetracker.presentation.navigation

sealed class Screen(val route: String) {
    object CoinList : Screen("coin_list")
    object Collections : Screen("collections")
    object CollectionDetail : Screen("collection_detail/{collectionId}") {
        fun createRoute(collectionId: Long) = "collection_detail/$collectionId"
    }
}

