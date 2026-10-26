package br.com.habil.carona.ui.rides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.local.RideFilterPrefs
import br.com.habil.carona.data.remote.toFriendlyMessage
import br.com.habil.carona.data.repository.AuthRepository
import br.com.habil.carona.data.repository.ContactRepository
import br.com.habil.carona.data.repository.RideRepository
import br.com.habil.carona.data.repository.SettingsRepository
import br.com.habil.carona.domain.model.Ride
import br.com.habil.carona.domain.model.RideFilter
import br.com.habil.carona.domain.model.RidePeriod
import br.com.habil.carona.domain.model.applyFilter
import br.com.habil.carona.ui.common.UiEvent
import br.com.habil.carona.ui.common.newEventFlow
import br.com.habil.carona.ui.common.showMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RidesUiState {
    data object Loading : RidesUiState
    data class Error(val message: String) : RidesUiState
    data class Success(
        val rides: List<Ride>,
        val filter: RideFilter,
        val isRefreshing: Boolean,
        val currentUserEmail: String?,
        val contactEmails: Set<String>,
        val hasAnyRides: Boolean,
    ) : RidesUiState
}

private sealed interface SyncState {
    data object Syncing : SyncState
    data object Idle : SyncState
    data class Failed(val message: String) : SyncState
}

class RideListViewModel(
    private val rideRepository: RideRepository,
    private val settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
    contactRepository: ContactRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    private val syncState = MutableStateFlow<SyncState>(SyncState.Syncing)
    private val _events = newEventFlow()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val identity = combine(
        authRepository.currentUser,
        contactRepository.observeContacts(),
    ) { user, contacts -> user?.email to contacts.map { it.email.lowercase() }.toSet() }

    val uiState: StateFlow<RidesUiState> = combine(
        rideRepository.observeRides(),
        settingsRepository.rideFilter,
        query,
        syncState,
        identity,
    ) { rides, prefs, query, sync, (userEmail, contactEmails) ->
        val now = System.currentTimeMillis()
        val upcoming = rides.filter { it.departureAt >= now }
        when {
            upcoming.isEmpty() && sync is SyncState.Syncing -> RidesUiState.Loading
            upcoming.isEmpty() && sync is SyncState.Failed -> RidesUiState.Error(sync.message)
            else -> {
                val filter = RideFilter(
                    query = query,
                    onlyAvailable = prefs.onlyAvailable,
                    period = prefs.period,
                    onlyContacts = prefs.onlyContacts,
                )
                RidesUiState.Success(
                    rides = rides.applyFilter(filter, now, contactEmails = contactEmails),
                    filter = filter,
                    isRefreshing = sync is SyncState.Syncing,
                    currentUserEmail = userEmail,
                    contactEmails = contactEmails,
                    hasAnyRides = upcoming.isNotEmpty(),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RidesUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            syncState.value = SyncState.Syncing
            rideRepository.refresh()
                .onSuccess { syncState.value = SyncState.Idle }
                .onFailure { error ->
                    val message = error.toFriendlyMessage()
                    syncState.value = SyncState.Failed(message)
                    if (uiState.value is RidesUiState.Success) _events.showMessage(message)
                }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onOnlyAvailableChange(value: Boolean) = updatePrefs { it.copy(onlyAvailable = value) }

    fun onOnlyContactsChange(value: Boolean) = updatePrefs { it.copy(onlyContacts = value) }

    fun onPeriodChange(value: RidePeriod) = updatePrefs { it.copy(period = value) }

    private fun updatePrefs(transform: (RideFilterPrefs) -> RideFilterPrefs) {
        viewModelScope.launch {
            settingsRepository.setRideFilter(transform(settingsRepository.rideFilter.first()))
        }
    }
}
