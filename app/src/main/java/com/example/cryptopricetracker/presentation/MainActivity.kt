package com.example.cryptopricetracker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.navigation.compose.rememberNavController
import com.example.cryptopricetracker.presentation.navigation.CryptoNavHost
import com.example.cryptopricetracker.ui.theme.CryptoPriceTrackerTheme
import com.example.cryptopricetracker.widget.CryptoCollectionWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CryptoPriceTrackerTheme {
                val navController = rememberNavController()
                // Pass window size class down for adaptive layouts
                val windowSizeClass = calculateWindowSizeClass(this)
                CryptoNavHost(
                    navController = navController,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh widgets when app comes to foreground so prices stay current
        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@MainActivity)
            val ids = manager.getGlanceIds(CryptoCollectionWidget::class.java)
            ids.forEach { CryptoCollectionWidget().update(this@MainActivity, it) }
        }
    }
}

