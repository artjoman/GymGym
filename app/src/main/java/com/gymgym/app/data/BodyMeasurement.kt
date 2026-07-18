package com.gymgym.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A logged body measurement. [type] is a [BodyMetric] name; [unit] is the unit in
 * effect when it was logged (kg/lb for weight, cm/in for lengths).
 */
@Entity(tableName = "body_measurement")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Double,
    val unit: String,
    val loggedAt: Long,
)

/** The trackable body metrics (weight + circumferences). */
enum class BodyMetric { WEIGHT, ARM, LEG, CHEST, SHOULDERS, CALVES, WAIST }

@Dao
interface BodyMeasurementDao {

    @Query("SELECT * FROM body_measurement ORDER BY loggedAt DESC")
    fun all(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurement ORDER BY loggedAt")
    suspend fun allOnce(): List<BodyMeasurement>

    @Insert
    suspend fun insert(measurement: BodyMeasurement): Long

    @Insert
    suspend fun insertAll(items: List<BodyMeasurement>)

    @Query("DELETE FROM body_measurement")
    suspend fun deleteAll()
}

class BodyMeasurementRepository(private val dao: BodyMeasurementDao) {

    val all: Flow<List<BodyMeasurement>> = dao.all()

    suspend fun log(type: BodyMetric, value: Double, unit: String) =
        dao.insert(
            BodyMeasurement(
                type = type.name,
                value = value,
                unit = unit,
                loggedAt = System.currentTimeMillis(),
            ),
        )

    suspend fun allOnce(): List<BodyMeasurement> = dao.allOnce()

    suspend fun replaceAll(items: List<BodyMeasurement>) {
        dao.deleteAll()
        dao.insertAll(items)
    }
}
