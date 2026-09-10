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

    fun startMonitoring(allApps: List<AppInfo>) {
        if (isMonitoring) return
        isMonitoring = true
        
        viewModelScope.launch {
            while (isMonitoring) {
                _activeApps.value = repository.getActiveApps(allApps)
                delay(10000) // Refresh every 10 seconds
            }
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
