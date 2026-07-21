package com.gymgym.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Badge art is a placeholder for now — the mascot inside an accent ring, greyed
 * out while locked. Per-achievement artwork lands in a follow-up pass (see
 * `.claude/agents/asset-designer.md`); only [AchievementBadge] changes then.
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
        AchievementBadge(earned = state.earned)
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

/** Placeholder badge: the mascot in an accent ring, desaturated while locked. */
@Composable
fun AchievementBadge(earned: Boolean, size: androidx.compose.ui.unit.Dp = 72.dp) {
    val ring = if (earned) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(3.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size).alpha(if (earned) 1f else 0.35f),
            // Locked badges are drained of colour so earned ones stand out.
            colorFilter = if (earned) {
                null
            } else {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            },
        )
    }
}
