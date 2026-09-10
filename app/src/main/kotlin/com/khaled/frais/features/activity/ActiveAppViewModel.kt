package com.khaled.frais.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khaled.frais.FraisApp
import com.khaled.frais.app.AppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveAppViewModel : ViewModel() {

    private val repository = ActiveAppRepository(FraisApp.app)
    
    private val _activeApps = MutableStateFlow<List<ActiveApp>>(emptyList())
    val activeApps = _activeApps.asStateFlow()

    private var isMonitoring = false
    private var lastAllApps: List<AppInfo>? = null

    fun startMonitoring(allApps: List<AppInfo>) {
        lastAllApps = allApps
        if (isMonitoring) return
        isMonitoring = true
        
        viewModelScope.launch {
            while (isMonitoring) {
                refreshInternal()
                delay(2000) // Improved refresh rate
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshInternal()
        }
    }

    private suspend fun refreshInternal() {
        lastAllApps?.let { allApps ->
            _activeApps.value = repository.getActiveApps(allApps)
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
