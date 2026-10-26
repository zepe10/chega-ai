package br.com.habil.carona.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.habil.carona.BuildConfig
import br.com.habil.carona.domain.model.ThemeMode
import br.com.habil.carona.domain.model.User
import br.com.habil.carona.domain.model.UserRole
import br.com.habil.carona.ui.components.CollectUiEvents
import br.com.habil.carona.ui.components.ConfirmDialog
import br.com.habil.carona.ui.components.PasswordTextField
import br.com.habil.carona.ui.components.UserAvatar

@Composable
fun ProfileRoute(viewModel: ProfileViewModel, onOpenContacts: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    CollectUiEvents(viewModel.events, snackbarHostState)

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri.toString())
    }

    ProfileScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        appVersion = BuildConfig.VERSION_NAME,
        onChangePhoto = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemovePhotoClick = viewModel::onRemovePhotoClick,
        onRemovePhotoConfirm = viewModel::onRemovePhotoConfirm,
        onRemovePhotoDismiss = viewModel::onRemovePhotoDismiss,
        onEditProfileClick = viewModel::onEditProfileClick,
        onEditNameChange = viewModel::onEditNameChange,
        onEditRoleChange = viewModel::onEditRoleChange,
        onEditCourseChange = viewModel::onEditCourseChange,
        onEditSave = viewModel::onEditSave,
        onEditDismiss = viewModel::onEditDismiss,
        onChangePasswordClick = viewModel::onChangePasswordClick,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onPasswordSave = viewModel::onPasswordSave,
        onPasswordDismiss = viewModel::onPasswordDismiss,
        onOpenContacts = onOpenContacts,
        onThemeSelected = viewModel::onThemeSelected,
        onLogoutClick = viewModel::onLogoutClick,
        onLogoutConfirm = viewModel::onLogoutConfirm,
        onLogoutDismiss = viewModel::onLogoutDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    appVersion: String,
    onChangePhoto: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onRemovePhotoConfirm: () -> Unit,
    onRemovePhotoDismiss: () -> Unit,
    onEditProfileClick: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onEditRoleChange: (UserRole) -> Unit,
    onEditCourseChange: (String) -> Unit,
    onEditSave: () -> Unit,
    onEditDismiss: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordSave: () -> Unit,
    onPasswordDismiss: () -> Unit,
    onOpenContacts: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onLogoutClick: () -> Unit,
    onLogoutConfirm: () -> Unit,
    onLogoutDismiss: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Perfil") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.user?.let { user ->
                ProfileHeader(
                    user = user,
                    isSavingPhoto = state.local.isSavingPhoto,
                    onChangePhoto = onChangePhoto,
                    onRemovePhoto = onRemovePhotoClick,
                    onEditProfile = onEditProfileClick,
                )
                AccountCard(email = user.email, onChangePassword = onChangePasswordClick)
            }

            ContactsCard(state = state, onOpenContacts = onOpenContacts)
            ThemeCard(themeMode = state.themeMode, onThemeSelected = onThemeSelected)
            AboutCard(appVersion = appVersion)

            OutlinedButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("  Sair da conta")
            }
        }
    }

    if (state.local.showLogout) {
        ConfirmDialog(
            title = "Sair da conta?",
            message = "Você precisará entrar novamente com seu e-mail institucional.",
            confirmText = "Sair",
            onConfirm = onLogoutConfirm,
            onDismiss = onLogoutDismiss,
            isDestructive = false,
        )
    }
    if (state.local.showRemovePhoto) {
        ConfirmDialog(
            title = "Remover foto de perfil?",
            message = "Suas iniciais voltarão a ser exibidas no lugar da foto.",
            confirmText = "Remover",
            onConfirm = onRemovePhotoConfirm,
            onDismiss = onRemovePhotoDismiss,
        )
    }
    state.local.editForm?.let { form ->
        EditProfileDialog(
            form = form,
            onNameChange = onEditNameChange,
            onRoleChange = onEditRoleChange,
            onCourseChange = onEditCourseChange,
            onSave = onEditSave,
            onDismiss = onEditDismiss,
        )
    }
    state.local.passwordForm?.let { form ->
        ChangePasswordDialog(
            form = form,
            onCurrentChange = onCurrentPasswordChange,
            onNewChange = onNewPasswordChange,
            onConfirmChange = onConfirmPasswordChange,
            onSave = onPasswordSave,
            onDismiss = onPasswordDismiss,
        )
    }
}

@Composable
private fun ProfileHeader(
    user: User,
    isSavingPhoto: Boolean,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onEditProfile: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserAvatar(name = user.name, photoPath = user.photoPath, size = 96.dp)
            Text(user.name, style = MaterialTheme.typography.titleLarge)
            if (user.affiliation.isKnown) {
                Text(
                    user.affiliation.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onChangePhoto, enabled = !isSavingPhoto) {
                    if (isSavingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text("  ${if (user.photoPath.isBlank()) "Adicionar foto" else "Trocar foto"}")
                }
                OutlinedButton(onClick = onEditProfile) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Editar")
                }
            }
            if (user.photoPath.isNotBlank()) {
                TextButton(onClick = onRemovePhoto) { Text("Remover foto", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun AccountCard(email: String, onChangePassword: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Conta", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("E-mail institucional", style = MaterialTheme.typography.labelMedium)
                    Text(email, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Não pode ser alterado: é ele que garante o acesso restrito à comunidade.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Senha", style = MaterialTheme.typography.labelMedium)
                    Text("••••••••", style = MaterialTheme.typography.bodyLarge)
                }
                OutlinedButton(onClick = onChangePassword) { Text("Alterar senha") }
            }
        }
    }
}

@Composable
private fun ContactsCard(state: ProfileUiState, onOpenContacts: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Contatos", style = MaterialTheme.typography.titleMedium)
            if (state.contacts.isEmpty()) {
                Text(
                    "Você ainda não adicionou ninguém.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("${state.contacts.size} contato(s) na sua lista", style = MaterialTheme.typography.bodyMedium)
                state.contacts.take(3).forEach { contact ->
                    Text(
                        text = listOf(contact.name, contact.affiliation.label)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.contacts.size > 3) {
                    Text(
                        "e mais ${state.contacts.size - 3}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(onClick = onOpenContacts) { Text("Ver e gerenciar contatos") }
        }
    }
}

@Composable
private fun ThemeCard(themeMode: ThemeMode, onThemeSelected: (ThemeMode) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .selectableGroup(),
        ) {
            Text("Tema do aplicativo", style = MaterialTheme.typography.titleMedium)
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = themeMode == mode,
                            onClick = { onThemeSelected(mode) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = themeMode == mode, onClick = null)
                    Text(mode.label, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun AboutCard(appVersion: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sobre o app", style = MaterialTheme.typography.titleMedium)
            Text("Chega Aí", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Caronas compartilhadas, seguras e sustentáveis entre alunos, professores e colaboradores da faculdade.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Versão $appVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
    form: EditProfileForm,
    onNameChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onCourseChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar perfil") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nome completo") },
                    singleLine = true,
                    isError = form.nameError != null,
                    supportingText = form.nameError?.let { { Text(it) } },
                )
                Text("Eu sou", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = form.role == role,
                            onClick = { onRoleChange(role) },
                            label = { Text(role.label) },
                        )
                    }
                }
                if (form.role == UserRole.STUDENT) {
                    OutlinedTextField(
                        value = form.course,
                        onValueChange = onCourseChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Curso") },
                        singleLine = true,
                        isError = form.courseError != null,
                        supportingText = form.courseError?.let { { Text(it) } },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !form.isSaving) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ChangePasswordDialog(
    form: ChangePasswordForm,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar senha") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PasswordTextField(
                    value = form.current,
                    onValueChange = onCurrentChange,
                    label = "Senha atual",
                    error = form.currentError,
                )
                PasswordTextField(
                    value = form.new,
                    onValueChange = onNewChange,
                    label = "Nova senha (mín. 6 caracteres)",
                    error = form.newError,
                )
                PasswordTextField(
                    value = form.confirm,
                    onValueChange = onConfirmChange,
                    label = "Confirmar nova senha",
                    error = form.confirmError,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !form.isSaving) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Alterar")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
