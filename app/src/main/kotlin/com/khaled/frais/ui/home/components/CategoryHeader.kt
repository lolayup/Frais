package com.khaled.frais.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaled.frais.ui.components.NothingDivider
import com.khaled.frais.ui.theme.NothingRed

@Composable
fun CategoryHeader(
    title: String,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showSystemToggle: Boolean = false,
    isShowingSystem: Boolean = false,
    onSystemToggle: () -> Unit = {}
) {
    val rotation by animateFloatAsState(if (isCollapsed) -90f else 0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(16.dp, 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showSystemToggle) {
                    IconButton(
                        onClick = { onSystemToggle() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isShowingSystem) Icons.Default.Dns else Icons.Default.Circle,
                            contentDescription = "System Apps",
                            tint = if (isShowingSystem) NothingRed else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).rotate(rotation)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        NothingDivider()
    }
}
