package com.sans.expensetracker.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A shared component to render category icons.
 * Migrated from Material Icons to Emojis.
 */
@Composable
fun CategoryIcon(
    icon: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp
) {
    val displayIcon = when (icon) {
        "restaurant" -> "🍴"
        "health_and_safety" -> "💊"
        "shopping_bag" -> "🛍️"
        "commute" -> "🚗"
        "language" -> "🌐"
        "category" -> "📁"
        else -> if (icon.isEmpty()) "📁" else icon
    }

    Text(
        text = displayIcon,
        fontSize = fontSize,
        modifier = modifier
    )
}
