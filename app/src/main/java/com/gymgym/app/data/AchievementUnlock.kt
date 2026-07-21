package com.gymgym.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * When an achievement was first earned. Progress itself is always recomputed from
 * history (see `achievement/Achievements.kt`) — only the moment of unlocking is
 * stored, so the "earned on" date stays stable and can't drift if the rules change.
 */
@Entity(tableName = "achievement_unlock")
data class AchievementUnlock(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)

@Dao
interface AchievementUnlockDao {

    @Query("SELECT * FROM achievement_unlock")
    fun all(): Flow<List<AchievementUnlock>>

    @Query("SELECT * FROM achievement_unlock")
    suspend fun allOnce(): List<AchievementUnlock>

    /** IGNORE so a re-evaluation never overwrites the original unlock date. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(unlock: AchievementUnlock)

    @Query("DELETE FROM achievement_unlock")
    suspend fun clear()
}

class AchievementUnlockRepository(private val dao: AchievementUnlockDao) {

    val all: Flow<List<AchievementUnlock>> = dao.all()

    suspend fun allOnce(): List<AchievementUnlock> = dao.allOnce()

    suspend fun unlock(id: String, at: Long) = dao.insert(AchievementUnlock(id, at))

    suspend fun clear() = dao.clear()
}
