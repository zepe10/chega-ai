package br.com.habil.carona.ui.rides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.habil.carona.domain.model.RidePeriod
import br.com.habil.carona.ui.common.UiEvent
import br.com.habil.carona.ui.components.EmptyState
import br.com.habil.carona.ui.components.ErrorState
import br.com.habil.carona.ui.components.LoadingState
import br.com.habil.carona.ui.components.RideCard
import kotlinx.coroutines.launch

@Composable
fun RideListRoute(
    viewModel: RideListViewModel,
    onRideClick: (String) -> Unit,
    onCreateRide: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is UiEvent.Message) {
                launch {
                    val result = snackbarHostState.showSnackbar(
                        message = event.text,
                        actionLabel = "Tentar novamente",
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.refresh()
                }
            }
        }
    }

    RideListScreen(
        state = state,
        searchQuery = searchQuery,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onOnlyAvailableChange = viewModel::onOnlyAvailableChange,
        onOnlyContactsChange = viewModel::onOnlyContactsChange,
        onPeriodChange = viewModel::onPeriodChange,
        onRefresh = viewModel::refresh,
        onRideClick = onRideClick,
        onCreateRide = onCreateRide,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideListScreen(
    state: RidesUiState,
    searchQuery: String,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onOnlyAvailableChange: (Boolean) -> Unit,
    onOnlyContactsChange: (Boolean) -> Unit,
    onPeriodChange: (RidePeriod) -> Unit,
    onRefresh: () -> Unit,
    onRideClick: (String) -> Unit,
    onCreateRide: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caronas") },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !(state is RidesUiState.Success && state.isRefreshing),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRide,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Oferecer carona") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state) {
            RidesUiState.Loading -> LoadingState(Modifier.padding(padding))
            is RidesUiState.Error -> ErrorState(
                message = state.message,
                onRetry = onRefresh,
                modifier = Modifier.padding(padding),
            )
            is RidesUiState.Success -> RideListContent(
                state = state,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                onOnlyAvailableChange = onOnlyAvailableChange,
                onOnlyContactsChange = onOnlyContactsChange,
                onPeriodChange = onPeriodChange,
                onRideClick = onRideClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun RideListContent(
    state: RidesUiState.Success,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onOnlyAvailableChange: (Boolean) -> Unit,
    onOnlyContactsChange: (Boolean) -> Unit,
    onPeriodChange: (RidePeriod) -> Unit,
    onRideClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Buscar por bairro, cidade ou motorista") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.filter.onlyAvailable,
                    onClick = { onOnlyAvailableChange(!state.filter.onlyAvailable) },
                    label = { Text("Com vagas") },
                )
            }
            item {
                FilterChip(
                    selected = state.filter.onlyContacts,
                    onClick = { onOnlyContactsChange(!state.filter.onlyContacts) },
                    label = { Text("Meus contatos") },
                )
            }
            items(RidePeriod.entries) { period ->
                FilterChip(
                    selected = state.filter.period == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(period.label) },
                )
            }
        }
        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.rides.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.DirectionsCar,
                        title = if (state.hasAnyRides) "Nenhuma carona com esses filtros" else "Nenhuma carona disponível",
                        message = if (state.hasAnyRides) {
                            "Ajuste a busca ou os filtros para ver mais opções."
                        } else {
                            "Seja o primeiro a oferecer uma carona para a comunidade."
                        },
                    )
                }
            } else {
                items(state.rides, key = { it.id }) { ride ->
                    RideCard(
                        ride = ride,
                        isMine = ride.driverEmail == state.currentUserEmail,
                        isContact = ride.driverEmail.lowercase() in state.contactEmails,
                        onClick = { onRideClick(ride.id) },
                    )
                }
            }
        }
    }
}
