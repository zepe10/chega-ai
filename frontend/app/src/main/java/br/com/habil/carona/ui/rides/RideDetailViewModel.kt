package br.com.habil.carona.ui.rides

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.remote.toFriendlyMessage
import br.com.habil.carona.data.repository.AuthRepository
import br.com.habil.carona.data.repository.ContactRepository
import br.com.habil.carona.data.repository.RequestRepository
import br.com.habil.carona.data.repository.RideRepository
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

sealed interface RideDetailDialog {
    data object DeleteRide : RideDetailDialog
    data class CancelMyRequest(val requestId: Long) : RideDetailDialog
    data class RejectRequest(val requestId: Long, val passengerName: String) : RideDetailDialog
}

sealed interface RideDetailUiState {
    data object Loading : RideDetailUiState
    data object NotFound : RideDetailUiState
    data class Content(
        val ride: Ride,
        val isDriver: Boolean,
        val myRequest: RideRequest?,
        val requests: List<RideRequest>,
        val dialog: RideDetailDialog?,
        val isBusy: Boolean,
        val isDriverContact: Boolean,
    ) : RideDetailUiState
}

class RideDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val rideRepository: RideRepository,
    private val requestRepository: RequestRepository,
    authRepository: AuthRepository,
    private val contactRepository: ContactRepository,
) : ViewModel() {

    private val rideId: String = checkNotNull(savedStateHandle[ARG_RIDE_ID])
    private val dialog = MutableStateFlow<RideDetailDialog?>(null)
    private val busy = MutableStateFlow(false)
    private val _events = newEventFlow()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val identity = combine(
        authRepository.currentUser,
        contactRepository.observeContacts(),
    ) { user, contacts -> user?.email to contacts.map { it.email.lowercase() }.toSet() }

    val uiState: StateFlow<RideDetailUiState> = combine(
        rideRepository.observeRide(rideId),
        identity,
        requestRepository.observeForRide(rideId),
        dialog,
        busy,
    ) { ride, (email, contactEmails), requests, dialog, busy ->
        if (ride == null) {
            RideDetailUiState.NotFound
        } else {
            RideDetailUiState.Content(
                ride = ride,
                isDriver = ride.driverEmail.lowercase() == email,
                myRequest = requests.lastOrNull { it.passengerEmail == email },
                requests = requests,
                dialog = dialog,
                isBusy = busy,
                isDriverContact = ride.driverEmail.lowercase() in contactEmails,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RideDetailUiState.Loading)

    fun onRequestSeat() = perform("Solicitação enviada! Combine os detalhes no chat.") {
        requestRepository.requestSeat(rideId)
    }

    fun onConfirmRequest(requestId: Long) = perform("Passageiro confirmado.") {
        requestRepository.confirm(requestId)
    }

    fun onAddDriverToContacts() {
        val content = uiState.value as? RideDetailUiState.Content ?: return
        viewModelScope.launch {
            contactRepository.addContact(
                email = content.ride.driverEmail,
                knownName = content.ride.driverName,
                knownAffiliation = content.ride.driverAffiliation,
            )
                .onSuccess { _events.showMessage("${it.name} foi adicionado(a) aos contatos.") }
                .onFailure { _events.showMessage(it.toFriendlyMessage()) }
        }
    }

    fun onDeleteRideClick() {
        dialog.value = RideDetailDialog.DeleteRide
    }

    fun onCancelMyRequestClick(requestId: Long) {
        dialog.value = RideDetailDialog.CancelMyRequest(requestId)
    }

    fun onRejectRequestClick(request: RideRequest) {
        dialog.value = RideDetailDialog.RejectRequest(request.id, request.passengerName)
    }

    fun onDialogDismiss() {
        dialog.value = null
    }

    fun onDialogConfirm() {
        val current = dialog.value ?: return
        dialog.value = null
        when (current) {
            RideDetailDialog.DeleteRide -> viewModelScope.launch {
                busy.value = true
                rideRepository.deleteRide(rideId)
                    .onSuccess {
                        _events.showMessage("Carona excluída.")
                        _events.tryEmit(UiEvent.NavigateBack)
                    }
                    .onFailure { _events.showMessage(it.toFriendlyMessage()) }
                busy.value = false
            }
            is RideDetailDialog.CancelMyRequest ->
                perform("Solicitação cancelada.") { requestRepository.cancel(current.requestId) }
            is RideDetailDialog.RejectRequest ->
                perform("Solicitação recusada.") { requestRepository.reject(current.requestId) }
        }
    }

    private fun perform(successMessage: String, action: suspend () -> Result<Any?>) {
        viewModelScope.launch {
            busy.value = true
            action()
                .onSuccess { _events.showMessage(successMessage) }
                .onFailure { _events.showMessage(it.toFriendlyMessage()) }
            busy.value = false
        }
    }

    companion object {
        const val ARG_RIDE_ID = "rideId"
    }
}
