package com.example.cryptopricetracker.presentation.collections

import androidx.compose.ui.graphics.Color

val avatarColors = listOf(
    Color(0xFFFFD580), // gold
    Color(0xFFB5C4FF), // lavender-blue
    Color(0xFFB5E0FF), // sky blue
    Color(0xFFFFB5C8), // pink
    Color(0xFFB5FFD9), // mint
    Color(0xFFE0B5FF), // purple
)

fun avatarColor(index: Int) = avatarColors[index % avatarColors.size]

