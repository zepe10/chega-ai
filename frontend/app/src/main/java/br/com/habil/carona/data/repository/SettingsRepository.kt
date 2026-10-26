package br.com.habil.carona.data.repository

import br.com.habil.carona.data.local.RideFilterPrefs
import br.com.habil.carona.data.local.UserPreferencesDataSource
import br.com.habil.carona.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val preferences: UserPreferencesDataSource) {
    val themeMode: Flow<ThemeMode> = preferences.themeMode
    val rideFilter: Flow<RideFilterPrefs> = preferences.rideFilter

    suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    suspend fun setRideFilter(filter: RideFilterPrefs) = preferences.setRideFilter(filter)
}
