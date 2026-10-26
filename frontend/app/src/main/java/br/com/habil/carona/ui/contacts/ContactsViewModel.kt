package br.com.habil.carona.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.remote.toFriendlyMessage
import br.com.habil.carona.data.repository.ContactRepository
import br.com.habil.carona.domain.model.Contact
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddContactForm(
    val email: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
)

sealed interface ContactsUiState {
    data object Loading : ContactsUiState
    data class Content(
        val contacts: List<Contact>,
        val addForm: AddContactForm?,
        val contactToRemove: Contact?,
    ) : ContactsUiState
}

class ContactsViewModel(private val contactRepository: ContactRepository) : ViewModel() {

    private val addForm = MutableStateFlow<AddContactForm?>(null)
    private val contactToRemove = MutableStateFlow<Contact?>(null)
    private val _events = newEventFlow()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<ContactsUiState> = combine(
        contactRepository.observeContacts(),
        addForm,
        contactToRemove,
    ) { contacts, form, toRemove ->
        ContactsUiState.Content(contacts = contacts, addForm = form, contactToRemove = toRemove)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsUiState.Loading)

    fun onAddClick() {
        addForm.value = AddContactForm()
    }

    fun onEmailChange(value: String) {
        addForm.update { it?.copy(email = value, error = null) }
    }

    fun onAddDismiss() {
        addForm.value = null
    }

    fun onAddConfirm() {
        val form = addForm.value ?: return
        if (form.isSaving) return
        viewModelScope.launch {
            addForm.update { it?.copy(isSaving = true, error = null) }
            contactRepository.addContact(form.email)
                .onSuccess { contact ->
                    addForm.value = null
                    _events.showMessage("${contact.name} foi adicionado(a) aos contatos.")
                }
                .onFailure { error ->
                    addForm.update { it?.copy(isSaving = false, error = error.toFriendlyMessage()) }
                }
        }
    }

    fun onRemoveClick(contact: Contact) {
        contactToRemove.value = contact
    }

    fun onRemoveDismiss() {
        contactToRemove.value = null
    }

    fun onRemoveConfirm() {
        val contact = contactToRemove.value ?: return
        contactToRemove.value = null
        viewModelScope.launch {
            contactRepository.removeContact(contact.email)
                .onSuccess { _events.showMessage("${contact.name} removido(a) dos contatos.") }
                .onFailure { _events.showMessage(it.toFriendlyMessage()) }
        }
    }
}
