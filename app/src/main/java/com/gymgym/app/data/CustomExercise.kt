package com.gymgym.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A user-created exercise. Custom exercises can't be AI rep-counted; they're
 * available for manual workout execution and (later) plan building.
 */
@Entity(tableName = "custom_exercise")
data class CustomExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Dao
interface CustomExerciseDao {

    @Query("SELECT * FROM custom_exercise ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<CustomExercise>>

    @Query("SELECT * FROM custom_exercise ORDER BY createdAt")
    suspend fun allOnce(): List<CustomExercise>

    @Insert
    suspend fun insert(exercise: CustomExercise): Long

    @Insert
    suspend fun insertAll(exercises: List<CustomExercise>)

    @Query("DELETE FROM custom_exercise WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM custom_exercise")
    suspend fun deleteAll()
}

class CustomExerciseRepository(private val dao: CustomExerciseDao) {

    val all: Flow<List<CustomExercise>> = dao.all()

    suspend fun add(name: String): Long =
        dao.insert(CustomExercise(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun allOnce(): List<CustomExercise> = dao.allOnce()

    /** Replace all custom exercises (used by backup import). */
    suspend fun replaceAll(items: List<CustomExercise>) {
        dao.deleteAll()
        dao.insertAll(items)
    }
}
