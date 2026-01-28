package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.GoalType
import com.ciclismo.portugal.domain.model.StravaGoal

@Entity(tableName = "strava_goals")
data class StravaGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // DISTANCE_KM, ACTIVITIES, ELEVATION_M, TIME_HOURS
    val targetValue: Float,
    val currentValue: Float,
    val year: Int
)

fun StravaGoalEntity.toDomain(): StravaGoal = StravaGoal(
    id = id,
    type = GoalType.valueOf(type),
    targetValue = targetValue,
    currentValue = currentValue,
    year = year
)

fun StravaGoal.toEntity(): StravaGoalEntity = StravaGoalEntity(
    id = id,
    type = type.name,
    targetValue = targetValue,
    currentValue = currentValue,
    year = year
)
