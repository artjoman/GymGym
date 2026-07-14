package com.gymgym.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

/**
 * Round, frosted-glass icon button/indicator for on-camera controls. Passing
 * [onClick] makes it interactive (with a springy press animation); omitting it
 * renders a static indicator.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    background: Color = Color(0x99101821),
    onClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "press",
    )
    val base = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .size(46.dp)
        .clip(CircleShape)
        .background(background)
        .border(1.dp, Color(0x33FFFFFF), CircleShape)
    val clickable = if (onClick != null) {
        base.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else {
        base
    }
    Box(modifier = clickable, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}
