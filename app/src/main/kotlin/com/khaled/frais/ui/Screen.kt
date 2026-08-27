package com.khaled.frais.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.khaled.frais.R

sealed class Screen(val route: String, val titleId: Int, val icon: ImageVector) {
    data object Home : Screen("home", R.string.title_home, Icons.Default.Home)
    data object Games : Screen("games", R.string.title_games, Icons.Default.Gamepad)
    data object PrivateSpace : Screen("private", R.string.title_private_space, Icons.Default.Lock)
    data object Widgets : Screen("widgets", R.string.title_widgets, Icons.Default.Widgets)
    data object Settings : Screen("settings", R.string.title_settings, Icons.Default.Settings)
}
