package br.com.habil.carona.ui.rides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.habil.carona.domain.model.RequestStatus
import br.com.habil.carona.domain.model.RideRequest
import br.com.habil.carona.ui.components.CollectUiEvents
import br.com.habil.carona.ui.components.ConfirmDialog
import br.com.habil.carona.ui.components.EmptyState
import br.com.habil.carona.ui.components.IconText
import br.com.habil.carona.ui.components.LoadingState
import br.com.habil.carona.ui.components.StatusChip
import br.com.habil.carona.ui.components.Tag
import br.com.habil.carona.ui.util.displayName
import br.com.habil.carona.ui.util.openRouteInMaps
import br.com.habil.carona.ui.util.toDateTimeText

@Composable
fun RideDetailRoute(
    viewModel: RideDetailViewModel,
    onBack: () -> Unit,
    onEditRide: (String) -> Unit,
    onOpenChat: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    CollectUiEvents(viewModel.events, snackbarHostState, onNavigateBack = onBack)

    RideDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEditRide = onEditRide,
        onOpenChat = onOpenChat,
        onRequestSeat = viewModel::onRequestSeat,
        onAddDriverToContacts = viewModel::onAddDriverToContacts,
        onDeleteRide = viewModel::onDeleteRideClick,
        onCancelMyRequest = viewModel::onCancelMyRequestClick,
        onConfirmRequest = viewModel::onConfirmRequest,
        onRejectRequest = viewModel::onRejectRequestClick,
        onDialogConfirm = viewModel::onDialogConfirm,
        onDialogDismiss = viewModel::onDialogDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    state: RideDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEditRide: (String) -> Unit,
    onOpenChat: (Long) -> Unit,
    onRequestSeat: () -> Unit,
    onAddDriverToContacts: () -> Unit,
    onDeleteRide: () -> Unit,
    onCancelMyRequest: (Long) -> Unit,
    onConfirmRequest: (Long) -> Unit,
    onRejectRequest: (RideRequest) -> Unit,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da carona") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state is RideDetailUiState.Content && state.isDriver) {
                        IconButton(onClick = { onEditRide(state.ride.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar carona")
                        }
                        IconButton(onClick = onDeleteRide) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir carona")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state) {
            RideDetailUiState.Loading -> LoadingState(Modifier.padding(padding))
            RideDetailUiState.NotFound -> EmptyState(
                icon = Icons.Default.SearchOff,
                title = "Carona não encontrada",
                message = "Ela pode ter sido excluída pelo motorista.",
                modifier = Modifier.padding(padding),
            )
            is RideDetailUiState.Content -> {
                RideDetailContent(
                    state = state,
                    onOpenChat = onOpenChat,
                    onRequestSeat = onRequestSeat,
                    onAddDriverToContacts = onAddDriverToContacts,
                    onCancelMyRequest = onCancelMyRequest,
                    onConfirmRequest = onConfirmRequest,
                    onRejectRequest = onRejectRequest,
                    modifier = Modifier.padding(padding),
                )
                RideDetailDialogs(state.dialog, onDialogConfirm, onDialogDismiss)
            }
        }
    }
}

@Composable
private fun RideDetailContent(
    state: RideDetailUiState.Content,
    onOpenChat: (Long) -> Unit,
    onRequestSeat: () -> Unit,
    onAddDriverToContacts: () -> Unit,
    onCancelMyRequest: (Long) -> Unit,
    onConfirmRequest: (Long) -> Unit,
    onRejectRequest: (RideRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ride = state.ride
    val context = LocalContext.current
    var mapsAvailable by remember { mutableStateOf(true) }
    Column(modifier = modifier.fillMaxSize()) {
        if (state.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconText(Icons.Default.TripOrigin, "Saída: ${ride.origin}", maxLines = 3)
                    IconText(Icons.Default.Place, "Destino: ${ride.destination}", maxLines = 3)
                    if (ride.route.isNotBlank()) IconText(Icons.Default.Route, ride.route, maxLines = 4)
                    IconText(Icons.Default.Schedule, ride.departureAt.toDateTimeText())
                    IconText(
                        Icons.Default.EventSeat,
                        if (ride.isFull) "Lotada (${ride.seatsTotal} vagas)" else "${ride.seatsAvailable} de ${ride.seatsTotal} vagas livres",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconText(
                            Icons.Default.Person,
                            "Motorista: ${displayName(ride.driverName, ride.driverAffiliation)}",
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                        )
                        if (!state.isDriver) {
                            if (state.isDriverContact) {
                                Tag(
                                    "Nos seus contatos",
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            } else {
                                TextButton(onClick = onAddDriverToContacts) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("  Adicionar")
                                }
                            }
                        }
                    }
                    if (ride.vehicle.isNotBlank()) IconText(Icons.Default.DirectionsCar, ride.vehicle)
                    if (ride.notes.isNotBlank()) IconText(Icons.AutoMirrored.Filled.Notes, ride.notes, maxLines = 6)
                }
            }

            OutlinedButton(
                onClick = { mapsAvailable = openRouteInMaps(context, ride.origin, ride.destination) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Ver rota no mapa")
            }
            if (!mapsAvailable) {
                Text(
                    "Nenhum aplicativo de mapas foi encontrado neste aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.isDriver) {
                DriverRequestsSection(
                    requests = state.requests,
                    onOpenChat = onOpenChat,
                    onConfirm = onConfirmRequest,
                    onReject = onRejectRequest,
                )
            } else {
                PassengerSection(
                    state = state,
                    onOpenChat = onOpenChat,
                    onRequestSeat = onRequestSeat,
                    onCancelMyRequest = onCancelMyRequest,
                )
            }
        }
    }
}

@Composable
private fun PassengerSection(
    state: RideDetailUiState.Content,
    onOpenChat: (Long) -> Unit,
    onRequestSeat: () -> Unit,
    onCancelMyRequest: (Long) -> Unit,
) {
    val request = state.myRequest
    val canRequest = (request == null || !request.status.isActive) &&
        !state.ride.isFull &&
        state.ride.departureAt > System.currentTimeMillis()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (request != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sua solicitação:", style = MaterialTheme.typography.titleSmall)
                StatusChip(request.status)
            }
        }
        if (request != null && request.status.isActive) {
            Button(onClick = { onOpenChat(request.id) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Text("  Conversar com o motorista")
            }
            OutlinedButton(
                onClick = { onCancelMyRequest(request.id) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancelar solicitação", color = MaterialTheme.colorScheme.error)
            }
        }
        if (canRequest) {
            Button(onClick = onRequestSeat, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text("Solicitar vaga")
            }
        } else if (request == null || !request.status.isActive) {
            Text(
                if (state.ride.isFull) "Todas as vagas já foram confirmadas." else "Esta carona não aceita novas solicitações.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DriverRequestsSection(
    requests: List<RideRequest>,
    onOpenChat: (Long) -> Unit,
    onConfirm: (Long) -> Unit,
    onReject: (RideRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Solicitações de vaga", style = MaterialTheme.typography.titleMedium)
        if (requests.isEmpty()) {
            Text(
                "Nenhuma solicitação por enquanto. Assim que alguém pedir uma vaga, ela aparece aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        requests.forEachIndexed { index, request ->
            if (index > 0) HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(request.passengerName, style = MaterialTheme.typography.titleSmall)
                    if (request.passengerAffiliation.isKnown) {
                        Text(
                            request.passengerAffiliation.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusChip(request.status)
                }
                if (request.status.isActive) {
                    IconButton(onClick = { onOpenChat(request.id) }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Abrir chat")
                    }
                }
                if (request.status == RequestStatus.PENDING) {
                    IconButton(onClick = { onConfirm(request.id) }) {
                        Icon(Icons.Default.Check, contentDescription = "Confirmar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (request.status.isActive) {
                    IconButton(onClick = { onReject(request) }) {
                        Icon(Icons.Default.Close, contentDescription = "Recusar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RideDetailDialogs(
    dialog: RideDetailDialog?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        RideDetailDialog.DeleteRide -> ConfirmDialog(
            title = "Excluir carona?",
            message = "A carona e todas as solicitações vinculadas serão removidas. Esta ação não pode ser desfeita.",
            confirmText = "Excluir",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is RideDetailDialog.CancelMyRequest -> ConfirmDialog(
            title = "Cancelar solicitação?",
            message = "Sua vaga será liberada para outros passageiros e o chat será encerrado.",
            confirmText = "Cancelar solicitação",
            dismissText = "Manter",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is RideDetailDialog.RejectRequest -> ConfirmDialog(
            title = "Recusar passageiro?",
            message = "${dialog.passengerName} será informado(a) de que a solicitação foi recusada e o chat será encerrado.",
            confirmText = "Recusar",
            dismissText = "Voltar",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}
