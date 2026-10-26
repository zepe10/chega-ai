package br.com.habil.carona.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.habil.carona.domain.model.RideRequest
import br.com.habil.carona.ui.components.CollectUiEvents
import br.com.habil.carona.ui.components.ConfirmDialog
import br.com.habil.carona.ui.components.EmptyState
import br.com.habil.carona.ui.components.IconText
import br.com.habil.carona.ui.components.LoadingState
import br.com.habil.carona.ui.components.RideCard
import br.com.habil.carona.ui.components.StatusChip
import br.com.habil.carona.ui.util.displayName
import br.com.habil.carona.ui.util.toDateTimeText

@Composable
fun ActivityRoute(
    viewModel: ActivityViewModel,
    onRideClick: (String) -> Unit,
    onOpenChat: (Long) -> Unit,
    onCreateRide: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    CollectUiEvents(viewModel.events, snackbarHostState)

    ActivityScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onRideClick = onRideClick,
        onOpenChat = onOpenChat,
        onCreateRide = onCreateRide,
        onCancelRequest = viewModel::onCancelRequestClick,
        onClearHistory = viewModel::onClearHistoryClick,
        onDialogConfirm = viewModel::onDialogConfirm,
        onDialogDismiss = viewModel::onDialogDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    state: ActivityUiState,
    snackbarHostState: SnackbarHostState,
    onRideClick: (String) -> Unit,
    onOpenChat: (Long) -> Unit,
    onCreateRide: () -> Unit,
    onCancelRequest: (RideRequest) -> Unit,
    onClearHistory: () -> Unit,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minha atividade") },
                actions = {
                    IconButton(onClick = onClearHistory) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Limpar histórico")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state) {
            ActivityUiState.Loading -> LoadingState(Modifier.padding(padding))
            is ActivityUiState.Content -> {
                ActivityContent(
                    state = state,
                    onRideClick = onRideClick,
                    onOpenChat = onOpenChat,
                    onCreateRide = onCreateRide,
                    onCancelRequest = onCancelRequest,
                    modifier = Modifier.padding(padding),
                )
                ActivityDialogs(state.dialog, onDialogConfirm, onDialogDismiss)
            }
        }
    }
}

@Composable
private fun ActivityContent(
    state: ActivityUiState.Content,
    onRideClick: (String) -> Unit,
    onOpenChat: (Long) -> Unit,
    onCreateRide: () -> Unit,
    onCancelRequest: (RideRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Oferecidas (${state.myRides.size})", "Solicitadas (${state.myRequests.size})")

    Column(modifier = modifier.fillMaxSize()) {
        MetricsPanel(state.metrics, modifier = Modifier.padding(16.dp))
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectedTab == 0) {
                if (state.myRides.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.DirectionsCar,
                            title = "Você ainda não ofereceu caronas",
                            message = "Compartilhe seu trajeto e ajude colegas a chegarem à faculdade.",
                            action = { Button(onClick = onCreateRide) { Text("Oferecer carona") } },
                        )
                    }
                } else {
                    items(state.myRides, key = { it.id }) { ride ->
                        RideCard(
                            ride = ride,
                            isMine = true,
                            pendingRequests = state.pendingByRide[ride.id] ?: 0,
                            onClick = { onRideClick(ride.id) },
                        )
                    }
                }
            } else {
                if (state.myRequests.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.EventSeat,
                            title = "Nenhuma solicitação feita",
                            message = "Procure uma carona na aba Caronas e peça uma vaga.",
                        )
                    }
                } else {
                    items(state.myRequests, key = { it.id }) { request ->
                        MyRequestCard(
                            request = request,
                            onClick = { onRideClick(request.rideId) },
                            onOpenChat = { onOpenChat(request.id) },
                            onCancel = { onCancelRequest(request) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsPanel(metrics: ActivityMetrics, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Caronas oferecidas", metrics.ridesOffered, Modifier.weight(1f))
            MetricCard("Passageiros confirmados", metrics.confirmedPassengers, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Pedidos a responder", metrics.pendingToAnswer, Modifier.weight(1f))
            MetricCard("Minhas solicitações", metrics.myRequests, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MyRequestCard(
    request: RideRequest,
    onClick: () -> Unit,
    onOpenChat: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconText(Icons.Default.TripOrigin, request.origin)
                    IconText(Icons.Default.Place, request.destination)
                }
                StatusChip(request.status)
            }
            IconText(Icons.Default.Schedule, request.departureAt.toDateTimeText())
            IconText(
                Icons.Default.Person,
                "Motorista: ${displayName(request.driverName, request.driverAffiliation)}",
                maxLines = 2,
            )
            if (request.status.isActive) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenChat) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                        Text("  Chat")
                    }
                    OutlinedButton(onClick = onCancel) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityDialogs(dialog: ActivityDialog?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (dialog) {
        null -> Unit
        is ActivityDialog.CancelRequest -> ConfirmDialog(
            title = "Cancelar solicitação?",
            message = "Você deixará de participar da carona ${dialog.description}. O chat será encerrado.",
            confirmText = "Cancelar solicitação",
            dismissText = "Manter",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        ActivityDialog.ClearHistory -> ConfirmDialog(
            title = "Limpar histórico?",
            message = "Serão removidas suas solicitações recusadas, canceladas ou de caronas que já partiram. Esta ação não pode ser desfeita.",
            confirmText = "Limpar",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}
