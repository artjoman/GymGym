package com.gymgym.app.data

import kotlinx.coroutines.flow.Flow

/** Thin seam over [WorkoutDao] so ViewModels don't touch Room types directly. */
class WorkoutRepository(private val dao: WorkoutDao) {

    val allSessions: Flow<List<WorkoutSession>> = dao.allSessions()

    val stats: Flow<List<ExerciseStat>> = dao.stats()

    suspend fun log(session: WorkoutSession) = dao.insert(session)
}
