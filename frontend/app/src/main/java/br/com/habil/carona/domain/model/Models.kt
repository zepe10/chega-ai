package br.com.habil.carona.domain.model

enum class UserRole(val label: String) {
    STUDENT("Estudante"),
    PROFESSOR("Professor(a)"),
    STAFF("Colaborador(a)"),
}

data class Affiliation(
    val role: UserRole? = null,
    val course: String = "",
) {
    val label: String
        get() = when (role) {
            UserRole.STUDENT -> if (course.isBlank()) "Estudante" else "Estudante de ${course.trim()}"
            UserRole.PROFESSOR -> "Professor(a)"
            UserRole.STAFF -> "Colaborador(a)"
            null -> ""
        }

    val isKnown: Boolean get() = role != null
}

data class User(
    val email: String,
    val name: String,
    val affiliation: Affiliation = Affiliation(),
    val photoPath: String = "",
)

enum class RequestStatus(val label: String) {
    PENDING("Pendente"),
    CONFIRMED("Confirmada"),
    REJECTED("Recusada"),
    CANCELED("Cancelada");

    val isActive: Boolean get() = this == PENDING || this == CONFIRMED
}

data class Ride(
    val id: String,
    val driverEmail: String,
    val driverName: String,
    val driverAffiliation: Affiliation,
    val origin: String,
    val destination: String,
    val route: String,
    val departureAt: Long,
    val seatsTotal: Int,
    val seatsTaken: Int,
    val vehicle: String,
    val notes: String,
    val synced: Boolean,
) {
    val seatsAvailable: Int get() = (seatsTotal - seatsTaken).coerceAtLeast(0)
    val isFull: Boolean get() = seatsAvailable == 0
}

data class RideDraft(
    val origin: String,
    val destination: String,
    val route: String,
    val departureAt: Long,
    val seatsTotal: Int,
    val vehicle: String,
    val notes: String,
)

data class RideRequest(
    val id: Long,
    val rideId: String,
    val origin: String,
    val destination: String,
    val departureAt: Long,
    val driverName: String,
    val driverEmail: String,
    val driverAffiliation: Affiliation,
    val passengerName: String,
    val passengerEmail: String,
    val passengerAffiliation: Affiliation,
    val status: RequestStatus,
    val createdAt: Long,
)

data class ChatMessage(
    val id: Long,
    val requestId: Long,
    val senderEmail: String,
    val senderName: String,
    val text: String,
    val sentAt: Long,
)

data class Contact(
    val email: String,
    val name: String,
    val affiliation: Affiliation,
    val addedAt: Long,
)

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Escuro"),
}

enum class RidePeriod(val label: String) {
    ANY("Qualquer dia"),
    TODAY("Hoje"),
    WEEK("Próximos 7 dias"),
}

data class RideFilter(
    val query: String = "",
    val onlyAvailable: Boolean = false,
    val period: RidePeriod = RidePeriod.ANY,
    val onlyContacts: Boolean = false,
)
