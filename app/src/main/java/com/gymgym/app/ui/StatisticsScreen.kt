package com.gymgym.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.cycle.CycleSummary
import com.gymgym.app.data.BodyMeasurement
import com.gymgym.app.data.CompletedWorkoutWithExercises
import com.gymgym.app.data.WorkoutSession
import kotlinx.coroutines.launch

/** Statistics + History unified under one screen with tabs. */
@Composable
fun StatisticsScreen(
    sessions: List<WorkoutSession>,
    completedWorkouts: List<CompletedWorkoutWithExercises>,
    bodyMeasurements: List<BodyMeasurement>,
    onOpenSession: (Long) -> Unit,
    onBack: () -> Unit,
    customNames: Map<String, String> = emptyMap(),
    cycles: List<CycleSummary> = emptyList(),
    initialTab: Int = 0,
    /** Expand the most recent completed cycle (when opened from the Last cycle card). */
    expandLastCycle: Boolean = false,
    workoutWeekdays: Map<Long, Int> = emptyMap(),
) {
    // Three tabs (Stats → Workouts → Cycles), switchable by tap or horizontal swipe.
    val pagerState = rememberPagerState(initialPage = initialTab.coerceIn(0, 2), pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Text(
                stringResource(R.string.statistics_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        val tabLabels = listOf(
            stringResource(R.string.stats_title),
            stringResource(R.string.history_workouts),
            stringResource(R.string.stats_cycles_tab),
        )
        TabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.padding(top = 8.dp)) {
            tabLabels.forEachIndexed { i, label ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    text = { Text(label) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp),
        ) { page ->
            when (page) {
                0 -> StatsContent(sessions, bodyMeasurements, Modifier.fillMaxSize())
                1 -> HistoryContent(sessions, completedWorkouts, onOpenSession, Modifier.fillMaxSize(), customNames, workoutWeekdays)
                else -> CyclesContent(cycles, Modifier.fillMaxSize(), customNames, expandLastCycle)
            }
        }
    }
}
