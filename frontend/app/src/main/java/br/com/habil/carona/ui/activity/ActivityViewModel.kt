package br.com.habil.carona.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.remote.toFriendlyMessage
import br.com.habil.carona.data.repository.RequestRepository
import br.com.habil.carona.data.repository.RideRepository
import br.com.habil.carona.domain.model.RequestStatus
import br.com.habil.carona.domain.model.Ride
import br.com.habil.carona.domain.model.RideRequest
import br.com.habil.carona.ui.common.UiEvent
import br.com.habil.carona.ui.common.newEventFlow
import br.com.habil.carona.ui.common.showMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActivityMetrics(
    val ridesOffered: Int,
    val confirmedPassengers: Int,
    val pendingToAnswer: Int,
    val myRequests: Int,
)

sealed interface ActivityDialog {
    data class CancelRequest(val requestId: Long, val description: String) : ActivityDialog
    data object ClearHistory : ActivityDialog
}

sealed interface ActivityUiState {
    data object Loading : ActivityUiState
    data class Content(
        val myRides: List<Ride>,
        val pendingByRide: Map<String, Int>,
        val myRequests: List<RideRequest>,
        val metrics: ActivityMetrics,
        val dialog: ActivityDialog?,
    ) : ActivityUiState
}

class ActivityViewModel(
    rideRepository: RideRepository,
    private val requestRepository: RequestRepository,
) : ViewModel() {

    private val dialog = MutableStateFlow<ActivityDialog?>(null)
    private val _events = newEventFlow()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<ActivityUiState> = combine(
        rideRepository.observeMyRides(),
        requestRepository.observeMyRequests(),
        requestRepository.observeReceivedRequests(),
        dialog,
    ) { myRides, myRequests, received, dialog ->
        ActivityUiState.Content(
            myRides = myRides,
            pendingByRide = received.filter { it.status == RequestStatus.PENDING }.groupingBy { it.rideId }.eachCount(),
            myRequests = myRequests,
            metrics = ActivityMetrics(
                ridesOffered = myRides.size,
                confirmedPassengers = received.count { it.status == RequestStatus.CONFIRMED },
                pendingToAnswer = received.count { it.status == RequestStatus.PENDING },
                myRequests = myRequests.size,
            ),
            dialog = dialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState.Loading)

    fun onCancelRequestClick(request: RideRequest) {
        dialog.value = ActivityDialog.CancelRequest(request.id, "${request.origin} → ${request.destination}")
    }

    fun onClearHistoryClick() {
        dialog.value = ActivityDialog.ClearHistory
    }

    fun onDialogDismiss() {
        dialog.value = null
    }

    fun onDialogConfirm() {
        val current = dialog.value ?: return
        dialog.value = null
        viewModelScope.launch {
            when (current) {
                is ActivityDialog.CancelRequest ->
                    requestRepository.cancel(current.requestId)
                        .onSuccess { _events.showMessage("Solicitação cancelada.") }
                        .onFailure { _events.showMessage(it.toFriendlyMessage()) }
                ActivityDialog.ClearHistory ->
                    requestRepository.clearHistory()
                        .onSuccess { count -> _events.showMessage("Histórico limpo ($count item(ns) removido(s)).") }
                        .onFailure { _events.showMessage(it.toFriendlyMessage()) }
            }
        }
    }
}
