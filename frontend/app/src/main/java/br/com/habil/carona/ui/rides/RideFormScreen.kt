package br.com.habil.carona.ui.rides

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.habil.carona.ui.components.CollectUiEvents
import br.com.habil.carona.ui.components.ConfirmDialog
import br.com.habil.carona.ui.components.LoadingState
import br.com.habil.carona.ui.util.toDateTimeText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun RideFormRoute(viewModel: RideFormViewModel, onClose: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    CollectUiEvents(viewModel.events, snackbarHostState, onNavigateBack = onClose)

    BackHandler(enabled = state.isDirty, onBack = viewModel::onBackRequested)

    RideFormScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = viewModel::onBackRequested,
        onOriginChange = viewModel::onOriginChange,
        onDestinationChange = viewModel::onDestinationChange,
        onRouteChange = viewModel::onRouteChange,
        onDepartureChange = viewModel::onDepartureChange,
        onSeatsChange = viewModel::onSeatsChange,
        onVehicleChange = viewModel::onVehicleChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::onSave,
        onDiscardConfirm = viewModel::onDiscardConfirm,
        onDiscardDismiss = viewModel::onDiscardDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideFormScreen(
    state: RideFormUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onRouteChange: (String) -> Unit,
    onDepartureChange: (Long) -> Unit,
    onSeatsChange: (String) -> Unit,
    onVehicleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardDismiss: () -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val fields = state.fields
    val errors = state.errors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar carona" else "Oferecer carona") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = fields.origin,
                    onValueChange = onOriginChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Saindo de (bairro/cidade)") },
                    singleLine = true,
                    isError = errors.origin != null,
                    supportingText = errors.origin?.let { { Text(it) } },
                )
                OutlinedTextField(
                    value = fields.destination,
                    onValueChange = onDestinationChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Destino") },
                    singleLine = true,
                    isError = errors.destination != null,
                    supportingText = errors.destination?.let { { Text(it) } },
                )
                OutlinedTextField(
                    value = fields.route,
                    onValueChange = onRouteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Rota aproximada (opcional)") },
                    placeholder = { Text("Ex.: Av. Brasil > Rua das Flores > Portaria 2") },
                    minLines = 2,
                    maxLines = 3,
                )
                DepartureField(
                    departureAt = fields.departureAt,
                    error = errors.departure,
                    onClick = { showPicker = true },
                )
                OutlinedTextField(
                    value = fields.seats,
                    onValueChange = onSeatsChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Vagas disponíveis") },
                    singleLine = true,
                    isError = errors.seats != null,
                    supportingText = errors.seats?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = fields.vehicle,
                    onValueChange = onVehicleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Veículo (modelo e cor)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = fields.notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Observações (opcional)") },
                    minLines = 2,
                    maxLines = 4,
                )
                Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (state.isEditing) "Salvar alterações" else "Publicar carona")
                    }
                }
            }
        }
    }

    if (showPicker) {
        DateTimePickerFlow(
            initialMillis = fields.departureAt,
            onDismiss = { showPicker = false },
            onConfirm = {
                showPicker = false
                onDepartureChange(it)
            },
        )
    }

    if (state.showDiscardDialog) {
        ConfirmDialog(
            title = "Descartar alterações?",
            message = "Você tem alterações não salvas neste formulário. Se sair agora, elas serão perdidas.",
            confirmText = "Descartar",
            dismissText = "Continuar editando",
            onConfirm = onDiscardConfirm,
            onDismiss = onDiscardDismiss,
        )
    }
}

@Composable
private fun DepartureField(departureAt: Long?, error: String?, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressed) {
        if (pressed) onClick()
    }
    OutlinedTextField(
        value = departureAt?.toDateTimeText().orEmpty(),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = { Text("Data e horário de saída") },
        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Escolher data e horário") },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerFlow(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initialDateTime = remember(initialMillis) {
        initialMillis?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
            ?: LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    }
    var pickedDate by rememberSaveable { mutableStateOf<Long?>(null) }

    if (pickedDate == null) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    enabled = dateState.selectedDateMillis != null,
                    onClick = { pickedDate = dateState.selectedDateMillis },
                ) { Text("Próximo") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = initialDateTime.hour,
            initialMinute = initialDateTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Horário de saída") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = Instant.ofEpochMilli(checkNotNull(pickedDate)).atZone(ZoneOffset.UTC).toLocalDate()
                        val millis = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute))
                            .atZone(zone).toInstant().toEpochMilli()
                        pickedDate = null
                        onConfirm(millis)
                    },
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pickedDate = null }) { Text("Voltar") }
            },
        )
    }
}
