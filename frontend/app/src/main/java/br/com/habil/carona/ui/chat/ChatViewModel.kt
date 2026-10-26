package br.com.habil.carona.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.remote.toFriendlyMessage
import br.com.habil.carona.data.repository.AuthRepository
import br.com.habil.carona.data.repository.ChatRepository
import br.com.habil.carona.data.repository.RequestRepository
import br.com.habil.carona.domain.model.Affiliation
import br.com.habil.carona.domain.model.ChatMessage
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

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data object NotFound : ChatUiState
    data class Content(
        val request: RideRequest,
        val messages: List<ChatMessage>,
        val draft: String,
        val currentEmail: String,
        val partnerName: String,
        val partnerAffiliation: Affiliation,
        val canSend: Boolean,
    ) : ChatUiState
}

class ChatViewModel(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    requestRepository: RequestRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val requestId: Long = checkNotNull(savedStateHandle[ARG_REQUEST_ID])
    private val draft = MutableStateFlow("")
    private val _events = newEventFlow()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<ChatUiState> = combine(
        requestRepository.observeById(requestId),
        chatRepository.observeMessages(requestId),
        authRepository.currentUser,
        draft,
    ) { request, messages, user, draft ->
        if (request == null || user == null) {
            ChatUiState.NotFound
        } else {
            val partnerIsDriver = user.email == request.passengerEmail
            ChatUiState.Content(
                request = request,
                messages = messages,
                draft = draft,
                currentEmail = user.email,
                partnerName = if (partnerIsDriver) request.driverName else request.passengerName,
                partnerAffiliation = if (partnerIsDriver) request.driverAffiliation else request.passengerAffiliation,
                canSend = request.status.isActive,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState.Loading)

    fun onDraftChange(value: String) {
        draft.value = value
    }

    fun onSend() {
        val text = draft.value.trim()
        if (text.isEmpty()) return
        draft.value = ""
        viewModelScope.launch {
            chatRepository.send(requestId, text).onFailure { error ->
                draft.value = text
                _events.showMessage(error.toFriendlyMessage())
            }
        }
    }

    companion object {
        const val ARG_REQUEST_ID = "requestId"
    }
}
