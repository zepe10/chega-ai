package br.com.habil.carona.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.habil.carona.data.remote.toFriendlyMessage
import br.com.habil.carona.data.repository.AuthRepository
import br.com.habil.carona.domain.model.UserRole
import br.com.habil.carona.domain.validation.EmailPolicy
import br.com.habil.carona.domain.validation.PasswordPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isLoading) return
        val emailError = EmailPolicy.validate(current.email)
        val passwordError = if (current.password.isEmpty()) "Informe a senha" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.login(current.email, current.password)
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.toFriendlyMessage()) }
                }
        }
    }
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: UserRole = UserRole.STUDENT,
    val course: String = "",
    val courseError: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }

    fun onConfirmChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, confirmError = null, errorMessage = null) }

    fun onRoleChange(value: UserRole) =
        _uiState.update { it.copy(role = value, courseError = null, errorMessage = null) }

    fun onCourseChange(value: String) =
        _uiState.update { it.copy(course = value, courseError = null, errorMessage = null) }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isLoading) return
        val nameError = if (current.name.trim().length < 3) "Informe seu nome completo" else null
        val emailError = EmailPolicy.validate(current.email)
        val passwordError = PasswordPolicy.validate(current.password)
        val confirmError = if (current.confirmPassword != current.password) "As senhas não conferem" else null
        val courseError =
            if (current.role == UserRole.STUDENT && current.course.isBlank()) "Informe o seu curso" else null
        if (listOf(nameError, emailError, passwordError, confirmError, courseError).any { it != null }) {
            _uiState.update {
                it.copy(
                    courseError = courseError,
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.register(current.name, current.email, current.password, current.role, current.course)
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.toFriendlyMessage()) }
                }
        }
    }
}
