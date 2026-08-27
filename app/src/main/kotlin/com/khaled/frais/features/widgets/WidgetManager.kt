package com.khaled.frais.features.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import com.khaled.frais.FraisApp
import com.khaled.frais.app.FraisData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FraisWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId)

object WidgetManager {
    private const val HOST_ID = 1024
    
    private val host by lazy { FraisWidgetHost(FraisApp.app, HOST_ID) }
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(FraisApp.app) }

    private val _widgetsState = MutableStateFlow<List<FraisData.WidgetMetadata>>(emptyList())
    val widgetsState = _widgetsState.asStateFlow()

    init {
        _widgetsState.value = FraisData.widgets.toList()
    }

    fun startListening() {
        try {
            host.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        try {
            host.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun allocateAppWidgetId(): Int {
        return host.allocateAppWidgetId()
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        host.deleteAppWidgetId(appWidgetId)
        synchronized(FraisData.widgets) {
            FraisData.widgets.removeAll { it.appWidgetId == appWidgetId }
        }
        FraisData.saveWidgets()
        _widgetsState.value = FraisData.widgets.toList()
    }

    fun addWidget(appWidgetId: Int, provider: String) {
        synchronized(FraisData.widgets) {
            // Avoid duplicates
            if (FraisData.widgets.none { it.appWidgetId == appWidgetId }) {
                FraisData.widgets.add(FraisData.WidgetMetadata(appWidgetId, provider))
            }
        }
        FraisData.saveWidgets()
        _widgetsState.value = FraisData.widgets.toList()
    }

    fun createView(context: Context, appWidgetId: Int, providerInfo: AppWidgetProviderInfo): AppWidgetHostView {
        return host.createView(context, appWidgetId, providerInfo)
    }

    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return appWidgetManager.getAppWidgetInfo(appWidgetId)
    }
    
    fun getInstalledProviders(): List<AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders
    }
    
    fun findProvider(packageName: String): AppWidgetProviderInfo? {
        return getInstalledProviders().find { it.provider.packageName == packageName }
    }

    fun bindWidget(appWidgetId: Int, provider: ComponentName): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
    }

    fun getInitialProviders(): List<AppWidgetProviderInfo> {
        val targets = listOf("com.duolingo", "com.remember.app", "com.remember")
        return getInstalledProviders().filter { provider ->
            targets.any { provider.provider.packageName.contains(it, ignoreCase = true) }
        }
    }
}
