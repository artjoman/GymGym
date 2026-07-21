package com.gymgym.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.achievement.AchievementState

/**
 * Profile → Achievements: every badge, earned first, with progress on the rest.
 *
 * Each badge is its own hand-prompted medal (see `.claude/agents/asset-designer.md`),
 * drained of colour while locked so the earned ones carry the eye.
 */
@Composable
fun AchievementsContent(
    achievements: List<AchievementState>,
    modifier: Modifier = Modifier,
) {
    if (achievements.isEmpty()) {
        Text(
            stringResource(R.string.achievements_empty),
            modifier = modifier.padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    // Earned first (most recent first), then the rest closest-to-earned first,
    // so the next thing worth chasing is near the top.
    val ordered = achievements.sortedWith(
        compareByDescending<AchievementState> { it.earned }
            .thenByDescending { it.unlockedAt ?: 0L }
            .thenByDescending { it.progress.toFloat() / it.def.target },
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(ordered, key = { it.def.id }) { state ->
            AchievementCell(state)
        }
    }
}

@Composable
private fun AchievementCell(state: AchievementState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AchievementBadge(badgeRes = state.def.badgeRes, earned = state.earned)
        Text(
            stringResource(state.def.nameRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = if (state.earned) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = if (state.earned) {
                state.unlockedAt?.let { stringResource(R.string.achievements_earned, formatDate(it)) }
                    ?: stringResource(state.def.descRes)
            } else {
                stringResource(R.string.achievements_progress, state.progress, state.def.target)
            },
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One medal. The art is drawn full-bleed with the coin's rim on the frame edge,
 * so clipping to a circle both rounds it off and discards the corners — no
 * transparency needed in the source PNG.
 */
@Composable
fun AchievementBadge(
    @DrawableRes badgeRes: Int,
    earned: Boolean,
    size: androidx.compose.ui.unit.Dp = 72.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(badgeRes),
            contentDescription = null,
            modifier = Modifier.size(size).alpha(if (earned) 1f else 0.4f),
            // Locked badges are drained of colour so earned ones stand out.
            colorFilter = if (earned) {
                null
            } else {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            },
        )
    }
}
