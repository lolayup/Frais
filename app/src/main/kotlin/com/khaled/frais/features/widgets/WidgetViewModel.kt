package com.khaled.frais.features.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khaled.frais.app.FraisData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WidgetViewModel : ViewModel() {
    val widgets: StateFlow<List<FraisData.WidgetMetadata>> = WidgetManager.widgetsState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeWidget(appWidgetId: Int) {
        WidgetManager.deleteAppWidgetId(appWidgetId)
    }
}
