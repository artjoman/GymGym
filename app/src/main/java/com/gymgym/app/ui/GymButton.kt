package com.gymgym.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymgym.app.ui.theme.BrandGreen
import com.gymgym.app.ui.theme.BrandGreenDeep

enum class GymButtonStyle { Primary, Secondary }

/**
 * Branded button with a springy press animation (scales down and dims while
 * held). Primary is a filled green with a soft glow; Secondary is a ghost
 * button with a green outline over the dark ground.
 */
@Composable
fun GymButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GymButtonStyle = GymButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "press-scale",
    )
    val shape = RoundedCornerShape(14.dp)
    val alpha = if (enabled) 1f else 0.4f

    val base = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }

    val styled = when (style) {
        GymButtonStyle.Primary -> base
            .shadow(if (pressed) 2.dp else 10.dp, shape, spotColor = BrandGreen, ambientColor = BrandGreen)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(BrandGreen, BrandGreenDeep)))
        GymButtonStyle.Secondary -> base
            .clip(shape)
            .background(Color(0x33101821))
            .border(BorderStroke(1.5.dp, BrandGreen.copy(alpha = 0.7f)), shape)
    }

    Box(
        modifier = styled
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(PaddingValues(horizontal = 22.dp, vertical = 15.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (style == GymButtonStyle.Primary) Color(0xFF06210F) else BrandGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
