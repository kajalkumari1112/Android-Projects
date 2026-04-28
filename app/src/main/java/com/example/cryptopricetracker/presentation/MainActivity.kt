package com.example.cryptopricetracker.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.cryptopricetracker.presentation.navigation.CryptoNavHost
import com.example.cryptopricetracker.presentation.navigation.Screen
import com.example.cryptopricetracker.ui.theme.CryptoPriceTrackerTheme
import com.example.cryptopricetracker.widget.CryptoCollectionWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Holds a route that should be navigated to once the NavHost is ready.
    // Using mutableStateOf so Compose observes changes even from onNewIntent.
    private var pendingRoute by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Capture any widget deep-link that cold-started the app
        pendingRoute = routeFromIntent(intent)

        setContent {
            CryptoPriceTrackerTheme {
                val navController = rememberNavController()
                val windowSizeClass = calculateWindowSizeClass(this)

                CryptoNavHost(
                    navController = navController,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass
                )

                // Navigate once the NavHost has finished its first composition
                LaunchedEffect(pendingRoute) {
                    val route = pendingRoute ?: return@LaunchedEffect
                    pendingRoute = null
                    navController.navigate(route) {
                        popUpTo(Screen.CoinList.route) { inclusive = false }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App is already running — set the pending route; LaunchedEffect will pick it up
        pendingRoute = routeFromIntent(intent)
    }

    /** Maps an incoming Intent to a Compose navigation route, or null if irrelevant. */
    private fun routeFromIntent(intent: Intent?): String? {
        val data: Uri = intent?.data ?: return null
        if (data.scheme != "crypto" || data.host != "collections") return null

        // crypto://collections/123  → CollectionDetail for id=123
        // crypto://collections      → Collections list
        val collectionId = data.pathSegments.firstOrNull()?.toLongOrNull()
        return if (collectionId != null)
            Screen.CollectionDetail.createRoute(collectionId)
        else
            Screen.Collections.route
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@MainActivity)
            val ids = manager.getGlanceIds(CryptoCollectionWidget::class.java)
            ids.forEach { CryptoCollectionWidget().update(this@MainActivity, it) }
        }
    }
}
