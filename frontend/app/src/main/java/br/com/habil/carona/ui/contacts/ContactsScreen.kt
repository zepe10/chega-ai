package br.com.habil.carona.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.habil.carona.domain.model.Contact
import br.com.habil.carona.ui.components.CollectUiEvents
import br.com.habil.carona.ui.components.ConfirmDialog
import br.com.habil.carona.ui.components.EmptyState
import br.com.habil.carona.ui.components.LoadingState
import br.com.habil.carona.ui.components.UserAvatar

@Composable
fun ContactsRoute(viewModel: ContactsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    CollectUiEvents(viewModel.events, snackbarHostState)

    ContactsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAddClick = viewModel::onAddClick,
        onEmailChange = viewModel::onEmailChange,
        onAddConfirm = viewModel::onAddConfirm,
        onAddDismiss = viewModel::onAddDismiss,
        onRemoveClick = viewModel::onRemoveClick,
        onRemoveConfirm = viewModel::onRemoveConfirm,
        onRemoveDismiss = viewModel::onRemoveDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    state: ContactsUiState,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onAddConfirm: () -> Unit,
    onAddDismiss: () -> Unit,
    onRemoveClick: (Contact) -> Unit,
    onRemoveConfirm: () -> Unit,
    onRemoveDismiss: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Contatos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Adicionar contato") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state) {
            ContactsUiState.Loading -> LoadingState(Modifier.padding(padding))
            is ContactsUiState.Content -> {
                ContactsList(
                    contacts = state.contacts,
                    onRemoveClick = onRemoveClick,
                    modifier = Modifier.padding(padding),
                )
                state.addForm?.let { form ->
                    AddContactDialog(form, onEmailChange, onAddConfirm, onAddDismiss)
                }
                state.contactToRemove?.let { contact ->
                    ConfirmDialog(
                        title = "Remover contato?",
                        message = "${contact.name} (${contact.email}) deixará de aparecer na sua lista de contatos.",
                        confirmText = "Remover",
                        onConfirm = onRemoveConfirm,
                        onDismiss = onRemoveDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsList(
    contacts: List<Contact>,
    onRemoveClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (contacts.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.People,
                    title = "Nenhum contato ainda",
                    message = "Adicione colegas pelo e-mail institucional para achar as caronas deles mais rápido.",
                )
            }
        } else {
            items(contacts, key = { it.email }) { contact ->
                ContactCard(contact = contact, onRemove = { onRemoveClick(contact) })
            }
        }
    }
}

@Composable
private fun ContactCard(contact: Contact, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UserAvatar(name = contact.name, photoPath = "", size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.titleSmall)
                if (contact.affiliation.isKnown) {
                    Text(
                        contact.affiliation.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    contact.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remover ${contact.name}")
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    form: AddContactForm,
    onEmailChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar contato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Informe o e-mail institucional de quem já tem conta no app.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = form.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("E-mail institucional") },
                    singleLine = true,
                    isError = form.error != null,
                    supportingText = form.error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !form.isSaving) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Adicionar")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
