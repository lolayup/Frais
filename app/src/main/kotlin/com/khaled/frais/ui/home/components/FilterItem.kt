package com.khaled.frais.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaled.frais.ui.home.viewmodel.FilterWithCount
import com.khaled.frais.ui.theme.NothingRed

@Composable
fun FilterItem(
    filterWithCount: FilterWithCount,
    isSelected: Boolean,
    showPulseDot: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val filter = filterWithCount.filter
    val haptics = LocalHapticFeedback.current
    var showFilterMenu by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showFilterMenu = true
                }
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraSmall,
            border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(filter.icon, fontSize = 14.sp)
                
                if (showLabel) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${filter.name.uppercase()} (${filterWithCount.unfrozenCount})",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (showPulseDot && filterWithCount.actionableRunningCount > 0) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(NothingRed.copy(alpha = alpha), CircleShape)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showFilterMenu,
            onDismissRequest = { showFilterMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.background).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
        ) {
            DropdownMenuItem(
                text = { Text("EDIT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                onClick = {
                    showFilterMenu = false
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) }
            )
            if (!filter.isBuiltIn) {
                DropdownMenuItem(
                    text = { Text("REMOVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NothingRed) },
                    onClick = {
                        showFilterMenu = false
                        onRemove()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = NothingRed) }
                )
            }
        }
    }
}
