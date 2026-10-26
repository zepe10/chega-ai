package br.com.habil.carona.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.repository.AuthRepository
import br.com.habil.carona.data.repository.SettingsRepository
import br.com.habil.carona.domain.model.ThemeMode
import br.com.habil.carona.domain.model.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val user: User) : SessionState
}

data class AppUiState(
    val session: SessionState = SessionState.Loading,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class AppViewModel(
    authRepository: AuthRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        authRepository.currentUser,
        settingsRepository.themeMode,
    ) { user, themeMode ->
        AppUiState(
            session = if (user == null) SessionState.LoggedOut else SessionState.LoggedIn(user),
            themeMode = themeMode,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())
}
