package com.example.cryptopricetracker.presentation.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.cryptopricetracker.presentation.coinlist.CoinListScreen
import com.example.cryptopricetracker.presentation.collections.CollectionsScreen
import com.example.cryptopricetracker.presentation.collections.CollectionDetailScreen

@Composable
fun CryptoNavHost(
    navController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CoinList.route
    ) {
        composable(Screen.CoinList.route) {
            CoinListScreen(
                onNavigateToCollections = { navController.navigate(Screen.Collections.route) },
                isExpandedScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded
            )
        }
        composable(Screen.Collections.route) {
            CollectionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onCollectionClick = { collectionId ->
                    navController.navigate(Screen.CollectionDetail.createRoute(collectionId))
                }
            )
        }
        composable(
            route = Screen.CollectionDetail.route,
            arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: return@composable
            CollectionDetailScreen(
                collectionId = collectionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
