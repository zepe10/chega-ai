package br.com.habil.carona.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.remote.toFriendlyMessage
import br.com.habil.carona.data.repository.AuthRepository
import br.com.habil.carona.data.repository.ContactRepository
import br.com.habil.carona.data.repository.ProfileRepository
import br.com.habil.carona.data.repository.SettingsRepository
import br.com.habil.carona.domain.model.Contact
import br.com.habil.carona.domain.model.ThemeMode
import br.com.habil.carona.domain.model.User
import br.com.habil.carona.domain.model.UserRole
import br.com.habil.carona.domain.validation.PasswordPolicy
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

data class EditProfileForm(
    val name: String,
    val role: UserRole,
    val course: String,
    val nameError: String? = null,
    val courseError: String? = null,
    val isSaving: Boolean = false,
)

data class ChangePasswordForm(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val currentError: String? = null,
    val newError: String? = null,
    val confirmError: String? = null,
    val isSaving: Boolean = false,
)

data class ProfileLocalState(
    val showLogout: Boolean = false,
    val showRemovePhoto: Boolean = false,
    val editForm: EditProfileForm? = null,
    val passwordForm: ChangePasswordForm? = null,
    val isSavingPhoto: Boolean = false,
)

data class ProfileUiState(
    val user: User? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val contacts: List<Contact> = emptyList(),
    val local: ProfileLocalState = ProfileLocalState(),
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    contactRepository: ContactRepository,
) : ViewModel() {

    private val local = MutableStateFlow(ProfileLocalState())
    private val _events = newEventFlow()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.currentUser,
        settingsRepository.themeMode,
        contactRepository.observeContacts(),
        local,
    ) { user, themeMode, contacts, local ->
        ProfileUiState(user = user, themeMode = themeMode, contacts = contacts, local = local)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun onPhotoPicked(sourceUri: String) {
        if (local.value.isSavingPhoto) return
        viewModelScope.launch {
            local.update { it.copy(isSavingPhoto = true) }
            profileRepository.setPhoto(sourceUri)
                .onSuccess { _events.showMessage("Foto de perfil atualizada.") }
                .onFailure { _events.showMessage(it.toFriendlyMessage()) }
            local.update { it.copy(isSavingPhoto = false) }
        }
    }

    fun onRemovePhotoClick() = local.update { it.copy(showRemovePhoto = true) }

    fun onRemovePhotoDismiss() = local.update { it.copy(showRemovePhoto = false) }

    fun onRemovePhotoConfirm() {
        local.update { it.copy(showRemovePhoto = false) }
        viewModelScope.launch {
            profileRepository.removePhoto()
                .onSuccess { _events.showMessage("Foto removida.") }
                .onFailure { _events.showMessage(it.toFriendlyMessage()) }
        }
    }

    fun onEditProfileClick() {
        val user = uiState.value.user ?: return
        local.update {
            it.copy(
                editForm = EditProfileForm(
                    name = user.name,
                    role = user.affiliation.role ?: UserRole.STUDENT,
                    course = user.affiliation.course,
                ),
            )
        }
    }

    fun onEditNameChange(value: String) = updateEdit { it.copy(name = value, nameError = null) }

    fun onEditRoleChange(value: UserRole) = updateEdit { it.copy(role = value, courseError = null) }

    fun onEditCourseChange(value: String) = updateEdit { it.copy(course = value, courseError = null) }

    fun onEditDismiss() = local.update { it.copy(editForm = null) }

    fun onEditSave() {
        val form = local.value.editForm ?: return
        if (form.isSaving) return
        val nameError = if (form.name.trim().length < 3) "Informe seu nome completo" else null
        val courseError = if (form.role == UserRole.STUDENT && form.course.isBlank()) "Informe o seu curso" else null
        if (nameError != null || courseError != null) {
            updateEdit { it.copy(nameError = nameError, courseError = courseError) }
            return
        }
        viewModelScope.launch {
            updateEdit { it.copy(isSaving = true) }
            profileRepository.updateProfile(form.name, form.role, form.course)
                .onSuccess {
                    local.update { it.copy(editForm = null) }
                    _events.showMessage("Perfil atualizado.")
                }
                .onFailure { error ->
                    updateEdit { it.copy(isSaving = false) }
                    _events.showMessage(error.toFriendlyMessage())
                }
        }
    }

    private fun updateEdit(transform: (EditProfileForm) -> EditProfileForm) =
        local.update { state -> state.copy(editForm = state.editForm?.let(transform)) }

    fun onChangePasswordClick() = local.update { it.copy(passwordForm = ChangePasswordForm()) }

    fun onCurrentPasswordChange(value: String) = updatePassword { it.copy(current = value, currentError = null) }

    fun onNewPasswordChange(value: String) = updatePassword { it.copy(new = value, newError = null) }

    fun onConfirmPasswordChange(value: String) = updatePassword { it.copy(confirm = value, confirmError = null) }

    fun onPasswordDismiss() = local.update { it.copy(passwordForm = null) }

    fun onPasswordSave() {
        val form = local.value.passwordForm ?: return
        if (form.isSaving) return
        val currentError = if (form.current.isEmpty()) "Informe a senha atual" else null
        val newError = PasswordPolicy.validate(form.new)
            ?: if (form.new == form.current) "A nova senha deve ser diferente da atual" else null
        val confirmError = if (form.confirm != form.new) "As senhas não conferem" else null
        if (currentError != null || newError != null || confirmError != null) {
            updatePassword {
                it.copy(currentError = currentError, newError = newError, confirmError = confirmError)
            }
            return
        }
        viewModelScope.launch {
            updatePassword { it.copy(isSaving = true) }
            profileRepository.changePassword(form.current, form.new)
                .onSuccess {
                    local.update { it.copy(passwordForm = null) }
                    _events.showMessage("Senha alterada com sucesso.")
                }
                .onFailure { error ->
                    updatePassword { it.copy(isSaving = false, currentError = error.toFriendlyMessage()) }
                }
        }
    }

    private fun updatePassword(transform: (ChangePasswordForm) -> ChangePasswordForm) =
        local.update { state -> state.copy(passwordForm = state.passwordForm?.let(transform)) }

    fun onLogoutClick() = local.update { it.copy(showLogout = true) }

    fun onLogoutDismiss() = local.update { it.copy(showLogout = false) }

    fun onLogoutConfirm() {
        local.update { it.copy(showLogout = false) }
        viewModelScope.launch { authRepository.logout() }
    }
}
