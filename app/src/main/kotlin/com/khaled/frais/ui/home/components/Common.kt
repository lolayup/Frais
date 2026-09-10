package com.khaled.frais.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

@Composable
fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, scale: Float = 1f) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.scale(scale)
    )
}
