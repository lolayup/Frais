package com.khaled.frais.features.activity

import com.khaled.frais.app.AppInfo
import androidx.compose.ui.graphics.Color

data class ActiveApp(
    val appInfo: AppInfo,
    val lastUsedTime: Long,
    val dominantColor: Color = Color.DarkGray
)
