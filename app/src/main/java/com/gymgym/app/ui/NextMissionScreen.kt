package com.gymgym.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.cycle.CycleLineStatus
import com.gymgym.app.cycle.CycleSummary
import com.gymgym.app.cycle.CycleWorkoutLine
import com.gymgym.app.cycle.DashboardState
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * "Let's go!" — the workout execution screen.
 *
 * The whole cycle is a compact bar strip (weekday above each bar) and the user
 * swipes through the workouts one at a time, so a long cycle stays readable. The
 * action buttons follow the *selected* workout's state:
 *  - current (first unfinished) → START + SKIP
 *  - future / skipped           → MAKE NEXT
 *  - executed                   → read-only
 */
@Composable
fun NextMissionScreen(
    dashboard: DashboardState,
    onStart: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onMakeNext: (Long) -> Unit,
    onBack: () -> Unit,
    currentCycle: CycleSummary? = null,
    customNames: Map<String, String> = emptyMap(),
) {
    val mission = dashboard.nextMission
    val workouts = currentCycle?.workouts.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        Text(stringResource(R.string.mission_title), style = MaterialTheme.typography.headlineSmall)

        if (mission == null || currentCycle == null || workouts.isEmpty()) {
            Text(
                stringResource(R.string.mission_no_plan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        // The current workout: the first one that is neither executed nor skipped.
        val currentIndex = workouts.indexOfFirst { it.status == CycleLineStatus.PENDING }
        val pagerState = rememberPagerState(
            initialPage = currentIndex.coerceAtLeast(0),
            pageCount = { workouts.size },
        )
        val scope = rememberCoroutineScope()
        val selectedIndex = pagerState.currentPage.coerceIn(0, workouts.lastIndex)
        val selected = workouts[selectedIndex]

        // After a reorder the promoted workout becomes current — follow it.
        LaunchedEffect(currentIndex) {
            if (currentIndex >= 0) pagerState.animateScrollToPage(currentIndex)
        }

        Text(
            "${currentCycle.cycleName} (${currentCycle.percent}%)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        mission.plannedDate?.let {
            Text(
                stringResource(R.string.mission_planned, formatMissionTime(it)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Tapping a bar jumps to that workout; the selected one is outlined.
        CycleBars(
            workouts = workouts,
            selectedIndex = selectedIndex,
            onSelect = { i -> scope.launch { pagerState.animateScrollToPage(i) } },
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            WorkoutPage(
                line = workouts[page],
                isCurrent = page == currentIndex,
                customNames = customNames,
            )
        }

        MissionActions(
            selected = selected,
            isCurrent = selectedIndex == currentIndex,
            onStart = { onStart(selected.workoutId) },
            onSkip = { onSkip(selected.workoutId) },
            onMakeNext = { onMakeNext(selected.workoutId) },
        )
    }
}

/** One swipeable page: the workout's name, state and full exercise breakdown. */
@Composable
private fun WorkoutPage(
    line: CycleWorkoutLine,
    isCurrent: Boolean,
    customNames: Map<String, String>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    line.name + line.weekday?.let { " · ${weekdayShort(it)}" }.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    workoutStateLabel(line, isCurrent),
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        line.status == CycleLineStatus.SKIPPED -> MaterialTheme.colorScheme.error
                        line.status == CycleLineStatus.DONE -> MaterialTheme.colorScheme.primary
                        isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            for (ex in line.exercises) {
                Text(
                    exerciseLineText(
                        name = exerciseRefName(ex.exerciseRef, customNames),
                        targetReps = ex.targetReps,
                        targetSets = ex.targetSets,
                        completedReps = ex.completedReps,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Buttons for the selected workout's state (executed workouts are read-only). */
@Composable
private fun MissionActions(
    selected: CycleWorkoutLine,
    isCurrent: Boolean,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    onMakeNext: () -> Unit,
) {
    var confirmSkip by remember { mutableStateOf(false) }
    if (confirmSkip) {
        ConfirmDialog(
            title = stringResource(R.string.mission_skip_title),
            message = selected.name,
            confirmText = stringResource(R.string.mission_skip),
            onConfirm = onSkip,
            onDismiss = { confirmSkip = false },
        )
    }
    when {
        selected.status == CycleLineStatus.DONE -> Unit // executed → read-only
        isCurrent -> {
            GymButton(
                text = stringResource(R.string.action_start),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
            GymButton(
                text = stringResource(R.string.mission_skip),
                onClick = { confirmSkip = true },
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Future or skipped → promote it to be the cycle's current workout.
        else -> GymButton(
            text = stringResource(R.string.mission_make_next),
            onClick = onMakeNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun workoutStateLabel(line: CycleWorkoutLine, isCurrent: Boolean): String = when {
    line.status == CycleLineStatus.DONE -> "${line.percent}%"
    line.status == CycleLineStatus.SKIPPED -> stringResource(R.string.cycle_skipped)
    isCurrent -> stringResource(R.string.mission_swap_current)
    else -> stringResource(R.string.cycle_status_pending)
}

/** Compact cycle strip: weekday above each bar, name below, selected one outlined. */
@Composable
private fun CycleBars(
    workouts: List<CycleWorkoutLine>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (workouts.any { it.weekday != null }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                workouts.forEach { w ->
                    Text(
                        w.weekday?.let { weekdayShort(it) }.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            workouts.forEachIndexed { i, w ->
                val color = when (w.status) {
                    CycleLineStatus.DONE -> MaterialTheme.colorScheme.primary
                    CycleLineStatus.SKIPPED -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    CycleLineStatus.PENDING -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                }
                val selectedBorder = if (i == selectedIndex) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .then(selectedBorder)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (w.status == CycleLineStatus.DONE) {
                        Text(
                            "${w.percent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            workouts.forEach { w ->
                Text(
                    w.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun formatMissionTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** Localized short weekday name for our 1=Mon..7=Sun encoding. */
internal fun weekdayShort(day: Int): String {
    val symbols = java.text.DateFormatSymbols.getInstance().shortWeekdays
    val calDay = if (day == 7) java.util.Calendar.SUNDAY else day + 1
    return symbols.getOrNull(calDay).orEmpty()
}
