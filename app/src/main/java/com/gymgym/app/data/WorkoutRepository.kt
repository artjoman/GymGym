package com.gymgym.app.data

import kotlinx.coroutines.flow.Flow

/** Thin seam over [WorkoutDao] so ViewModels don't touch Room types directly. */
class WorkoutRepository(private val dao: WorkoutDao) {

    val allSessions: Flow<List<WorkoutSession>> = dao.allSessions()

    val stats: Flow<List<ExerciseStat>> = dao.stats()

    suspend fun log(session: WorkoutSession) = dao.insert(session)

    suspend fun allOnce(): List<WorkoutSession> = dao.getAllOnce()

    /** Replace all sessions (used by backup import). */
    suspend fun replaceAll(sessions: List<WorkoutSession>) {
        dao.deleteAll()
        if (sessions.isNotEmpty()) dao.insertAll(sessions)
    }
}
