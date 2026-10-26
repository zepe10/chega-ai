package br.com.habil.carona.domain.validation

const val MAX_SEATS = 8

data class RideFormErrors(
    val origin: String? = null,
    val destination: String? = null,
    val departure: String? = null,
    val seats: String? = null,
) {
    val hasErrors: Boolean get() = listOf(origin, destination, departure, seats).any { it != null }
}

fun validateRideForm(
    origin: String,
    destination: String,
    departureAt: Long?,
    seatsText: String,
    now: Long,
    minSeats: Int = 1,
): RideFormErrors {
    val seats = seatsText.trim().toIntOrNull()
    return RideFormErrors(
        origin = if (origin.isBlank()) "Informe o ponto de saída" else null,
        destination = if (destination.isBlank()) "Informe o destino" else null,
        departure = when {
            departureAt == null -> "Escolha data e horário de saída"
            departureAt <= now -> "O horário precisa estar no futuro"
            else -> null
        },
        seats = when {
            seats == null -> "Informe o número de vagas"
            seats < minSeats -> "Mínimo de $minSeats vaga(s)"
            seats > MAX_SEATS -> "Máximo de $MAX_SEATS vagas"
            else -> null
        },
    )
}
