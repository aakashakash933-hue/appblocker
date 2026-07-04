package com.appblocker.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.appblocker.core.ui.theme.AppBlockerBrushes
import com.appblocker.core.ui.theme.Dimensions

enum class ShieldState {
    Active,
    Warning,
    Blocked
}

@Composable
fun StatusShield(
    state: ShieldState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldPulse")
    
    // Pulse animations
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )

    val colorScheme = MaterialTheme.colorScheme
    val shieldColors = when (state) {
        ShieldState.Active -> Pair(colorScheme.primary, AppBlockerBrushes.PremiumShieldGradient)
        ShieldState.Warning -> Pair(colorScheme.tertiary, AppBlockerBrushes.WarningShieldGradient)
        ShieldState.Blocked -> Pair(colorScheme.error, AppBlockerBrushes.BlockedShieldGradient)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(Dimensions.ShieldSize)
    ) {
        // Glowing Canvas
        Canvas(modifier = Modifier.size(Dimensions.ShieldSize)) {
            val center = this.center
            val radius = size.minDimension / 2.5f

            // Drawing animated pulse ring
            drawCircle(
                color = shieldColors.first,
                radius = radius * pulseScale,
                center = center,
                alpha = pulseAlpha,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Draw a second pulse ring slightly offset
            drawCircle(
                color = shieldColors.first,
                radius = radius * (1f + (pulseScale - 1f) * 0.5f),
                center = center,
                alpha = pulseAlpha * 1.2f,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Inside Circle
        Box(
            modifier = Modifier
                .size(Dimensions.ShieldSize / 1.5f)
                .clip(CircleShape)
                .background(shieldColors.second),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (state) {
                ShieldState.Active -> Icons.Default.Shield
                ShieldState.Warning -> Icons.Default.Info
                ShieldState.Blocked -> Icons.Default.Block
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
