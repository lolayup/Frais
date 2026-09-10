package com.khaled.frais.features.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Base interface for all Frais native widgets.
 */
interface FraisWidget {
    val id: String
    val name: String
    val icon: String
    
    @Composable
    fun Content(modifier: Modifier)
}

/**
 * Metadata for a generic Frais widget (can be native or Android).
 */
sealed class WidgetType {
    data class Android(val appWidgetId: Int, val provider: String) : WidgetType()
    data class Native(val widgetId: String) : WidgetType()
}
