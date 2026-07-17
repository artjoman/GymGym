package com.gymgym.app.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymgym.app.ui.theme.LocalBrand

enum class GymButtonStyle { Primary, Secondary }

/**
 * Branded button, accent-aware (colors follow the selected color scheme).
 *
 * Primary is a filled accent with a soft glow, a lit top sheen, and a springy
 * press: it scales down with a slight overshoot, the glow contracts, and a thin
 * highlight flashes. Secondary is a ghost button with an accent outline that
 * fills subtly while held.
 */
@Composable
fun GymButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GymButtonStyle = GymButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val brand = LocalBrand.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press-scale",
    )
    val glow by animateDpAsState(
        targetValue = if (pressed) 2.dp else 12.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "press-glow",
    )
    // A brief "lit" flash while held, layered over the fill.
    val flash by animateFloatAsState(
        targetValue = if (pressed) 0.16f else 0f,
        animationSpec = spring(stiffness = 900f),
        label = "press-flash",
    )

    val shape = RoundedCornerShape(16.dp)
    val alpha = if (enabled) 1f else 0.4f
    val top = lerp(brand.accent, Color.White, 0.16f)

    val base = modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }

    val styled = when (style) {
        GymButtonStyle.Primary -> base
            .shadow(glow, shape, spotColor = brand.accent, ambientColor = brand.accent)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, brand.accent, brand.accentDeep)))
        GymButtonStyle.Secondary -> base
            .clip(shape)
            .background(brand.accent.copy(alpha = if (pressed) 0.22f else 0.10f))
            .border(BorderStroke(1.5.dp, brand.accent.copy(alpha = 0.75f)), shape)
    }

    Box(
        modifier = styled
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(PaddingValues(horizontal = 22.dp, vertical = 16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (style == GymButtonStyle.Primary) {
            // Top sheen: a soft white highlight over the upper half for a glossy,
            // premium fill instead of a flat color.
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.22f),
                        0.5f to Color.Transparent,
                    ),
                ),
            )
        }
        if (flash > 0f) {
            Box(Modifier.matchParentSize().background(Color.White.copy(alpha = flash)))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (style == GymButtonStyle.Primary) brand.onAccent else brand.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
