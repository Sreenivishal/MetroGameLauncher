package com.xbox.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Simulates the Windows Phone / Zune UI "tilt" effect.
 * Elements scale down slightly and reduce opacity when pressed,
 * without displaying standard Material ripples.
 */
fun Modifier.metroClickable(
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "MetroTiltScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "MetroTiltAlpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Removes the standard Material Ripple
            onClick = onClick
        )
}
