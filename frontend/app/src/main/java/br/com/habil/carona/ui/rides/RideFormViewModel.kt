package br.com.habil.carona.ui.rides

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.remote.toFriendlyMessage
import br.com.habil.carona.data.repository.AuthRepository
import br.com.habil.carona.data.repository.RideRepository
import br.com.habil.carona.domain.model.RideDraft
import br.com.habil.carona.domain.validation.RideFormErrors
import br.com.habil.carona.domain.validation.validateRideForm
import br.com.habil.carona.ui.common.UiEvent
import br.com.habil.carona.ui.common.newEventFlow
import br.com.habil.carona.ui.common.showMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RideFormFields(
    val origin: String = "",
    val destination: String = "",
    val route: String = "",
    val departureAt: Long? = null,
    val seats: String = "1",
    val vehicle: String = "",
    val notes: String = "",
)

data class RideFormUiState(
    val fields: RideFormFields = RideFormFields(),
    val errors: RideFormErrors = RideFormErrors(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val isDirty: Boolean = false,
)

class RideFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val rideRepository: RideRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val editingId: String? = savedStateHandle[ARG_RIDE_ID]
    private var initialFields = RideFormFields()
    private var minSeats = 1

    private val _uiState = MutableStateFlow(RideFormUiState(isEditing = editingId != null, isLoading = editingId != null))
    val uiState: StateFlow<RideFormUiState> = _uiState.asStateFlow()

    private val _events = newEventFlow()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        if (editingId != null) loadRide(editingId)
    }

    private fun loadRide(id: String) {
        viewModelScope.launch {
            val ride = rideRepository.getRide(id)
            val user = authRepository.requireUser()
            if (ride == null || ride.driverEmail.lowercase() != user.email) {
                _events.showMessage("Carona não encontrada ou sem permissão para editar.")
                _events.tryEmit(UiEvent.NavigateBack)
                return@launch
            }
            minSeats = maxOf(1, ride.seatsTaken)
            initialFields = RideFormFields(
                origin = ride.origin,
                destination = ride.destination,
                route = ride.route,
                departureAt = ride.departureAt,
                seats = ride.seatsTotal.toString(),
                vehicle = ride.vehicle,
                notes = ride.notes,
            )
            _uiState.update { it.copy(fields = initialFields, isLoading = false, isDirty = false) }
        }
    }

    fun onOriginChange(value: String) = updateFields({ it.copy(origin = value) }) { it.copy(origin = null) }

    fun onDestinationChange(value: String) =
        updateFields({ it.copy(destination = value) }) { it.copy(destination = null) }

    fun onRouteChange(value: String) = updateFields({ it.copy(route = value) })

    fun onDepartureChange(epochMillis: Long) =
        updateFields({ it.copy(departureAt = epochMillis) }) { it.copy(departure = null) }

    fun onSeatsChange(value: String) =
        updateFields({ it.copy(seats = value.filter(Char::isDigit).take(1)) }) { it.copy(seats = null) }

    fun onVehicleChange(value: String) = updateFields({ it.copy(vehicle = value) })

    fun onNotesChange(value: String) = updateFields({ it.copy(notes = value) })

    private fun updateFields(
        transform: (RideFormFields) -> RideFormFields,
        clearError: (RideFormErrors) -> RideFormErrors = { it },
    ) {
        _uiState.update { state ->
            val fields = transform(state.fields)
            state.copy(fields = fields, errors = clearError(state.errors), isDirty = fields != initialFields)
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val fields = state.fields
        val errors = validateRideForm(
            origin = fields.origin,
            destination = fields.destination,
            departureAt = fields.departureAt,
            seatsText = fields.seats,
            now = System.currentTimeMillis(),
            minSeats = minSeats,
        )
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        val draft = RideDraft(
            origin = fields.origin,
            destination = fields.destination,
            route = fields.route,
            departureAt = checkNotNull(fields.departureAt),
            seatsTotal = fields.seats.toInt(),
            vehicle = fields.vehicle,
            notes = fields.notes,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            rideRepository.saveRide(draft, editingId)
                .onSuccess {
                    initialFields = fields
                    _uiState.update { it.copy(isSaving = false, isDirty = false) }
                    _events.showMessage(if (editingId == null) "Carona publicada!" else "Carona atualizada.")
                    _events.tryEmit(UiEvent.NavigateBack)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _events.showMessage(error.toFriendlyMessage())
                }
        }
    }

    fun onBackRequested() {
        if (_uiState.value.isDirty) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _events.tryEmit(UiEvent.NavigateBack)
        }
    }

    fun onDiscardConfirm() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        _events.tryEmit(UiEvent.NavigateBack)
    }

    fun onDiscardDismiss() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    companion object {
        const val ARG_RIDE_ID = "rideId"
    }
}
