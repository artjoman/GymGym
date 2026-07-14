package com.gymgym.app.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile")

enum class WeightUnit(val label: String) { KG("Kilograms (kg)"), LB("Pounds (lb)") }

data class Profile(
    val displayName: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
)

class ProfileRepository(context: Context) {

    private val dataStore = context.applicationContext.profileDataStore

    val profile: Flow<Profile> = dataStore.data.map { prefs ->
        Profile(
            displayName = prefs[DISPLAY_NAME] ?: "",
            weightUnit = prefs[WEIGHT_UNIT]
                ?.let { stored -> WeightUnit.entries.find { it.name == stored } }
                ?: WeightUnit.KG,
        )
    }

    suspend fun setDisplayName(value: String) = dataStore.edit { it[DISPLAY_NAME] = value }

    suspend fun setWeightUnit(unit: WeightUnit) = dataStore.edit { it[WEIGHT_UNIT] = unit.name }

    private companion object {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    }
}
