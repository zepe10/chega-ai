package br.com.habil.carona.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.habil.carona.domain.model.RidePeriod
import br.com.habil.carona.domain.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class RideFilterPrefs(
    val onlyAvailable: Boolean = false,
    val period: RidePeriod = RidePeriod.ANY,
    val onlyContacts: Boolean = false,
)

class UserPreferencesDataSource(private val dataStore: DataStore<Preferences>) {

    private val data: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val sessionEmail: Flow<String?> = data.map { it[SESSION_EMAIL] }

    val themeMode: Flow<ThemeMode> = data.map { prefs ->
        prefs[THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.SYSTEM
    }

    val rideFilter: Flow<RideFilterPrefs> = data.map { prefs ->
        RideFilterPrefs(
            onlyAvailable = prefs[FILTER_ONLY_AVAILABLE] ?: false,
            onlyContacts = prefs[FILTER_ONLY_CONTACTS] ?: false,
            period = prefs[FILTER_PERIOD]?.let { stored -> RidePeriod.entries.firstOrNull { it.name == stored } }
                ?: RidePeriod.ANY,
        )
    }

    suspend fun setSessionEmail(email: String?) {
        dataStore.edit { prefs ->
            if (email == null) prefs.remove(SESSION_EMAIL) else prefs[SESSION_EMAIL] = email
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setRideFilter(filter: RideFilterPrefs) {
        dataStore.edit {
            it[FILTER_ONLY_AVAILABLE] = filter.onlyAvailable
            it[FILTER_ONLY_CONTACTS] = filter.onlyContacts
            it[FILTER_PERIOD] = filter.period.name
        }
    }

    private companion object {
        val SESSION_EMAIL = stringPreferencesKey("session_email")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FILTER_ONLY_AVAILABLE = booleanPreferencesKey("filter_only_available")
        val FILTER_ONLY_CONTACTS = booleanPreferencesKey("filter_only_contacts")
        val FILTER_PERIOD = stringPreferencesKey("filter_period")
    }
}
