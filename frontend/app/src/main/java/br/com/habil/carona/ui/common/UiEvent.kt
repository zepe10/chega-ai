package br.com.habil.carona.ui.common

import kotlinx.coroutines.flow.MutableSharedFlow

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data object NavigateBack : UiEvent
}

fun MutableSharedFlow<UiEvent>.showMessage(text: String) {
    tryEmit(UiEvent.Message(text))
}

fun newEventFlow(): MutableSharedFlow<UiEvent> = MutableSharedFlow(extraBufferCapacity = 8)
