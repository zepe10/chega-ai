package br.com.habil.carona.data.mapper

import br.com.habil.carona.data.local.entity.ContactEntity
import br.com.habil.carona.data.local.entity.MessageEntity
import br.com.habil.carona.data.local.entity.RequestWithRide
import br.com.habil.carona.data.local.entity.RideEntity
import br.com.habil.carona.data.local.entity.RideWithSeats
import br.com.habil.carona.data.local.entity.UserEntity
import br.com.habil.carona.data.remote.RideDto
import br.com.habil.carona.domain.model.Affiliation
import br.com.habil.carona.domain.model.ChatMessage
import br.com.habil.carona.domain.model.Contact
import br.com.habil.carona.domain.model.RequestStatus
import br.com.habil.carona.domain.model.Ride
import br.com.habil.carona.domain.model.RideRequest
import br.com.habil.carona.domain.model.User
import br.com.habil.carona.domain.model.UserRole
import kotlinx.serialization.json.JsonPrimitive

fun affiliationOf(role: String, course: String): Affiliation =
    Affiliation(
        role = UserRole.entries.firstOrNull { it.name == role },
        course = course,
    )

fun UserEntity.toDomain() = User(
    email = email,
    name = name,
    affiliation = affiliationOf(role, course),
    photoPath = photoPath,
)

fun RideWithSeats.toDomain() = Ride(
    id = ride.id,
    driverEmail = ride.driverEmail,
    driverName = ride.driverName,
    driverAffiliation = affiliationOf(ride.driverRole, ride.driverCourse),
    origin = ride.origin,
    destination = ride.destination,
    route = ride.route,
    departureAt = ride.departureAt,
    seatsTotal = ride.seatsTotal,
    seatsTaken = seatsTaken,
    vehicle = ride.vehicle,
    notes = ride.notes,
    synced = ride.synced,
)

fun RequestWithRide.toDomain() = RideRequest(
    id = request.id,
    rideId = ride.id,
    origin = ride.origin,
    destination = ride.destination,
    departureAt = ride.departureAt,
    driverName = ride.driverName,
    driverEmail = ride.driverEmail,
    driverAffiliation = affiliationOf(ride.driverRole, ride.driverCourse),
    passengerName = request.passengerName,
    passengerEmail = request.passengerEmail,
    passengerAffiliation = affiliationOf(request.passengerRole, request.passengerCourse),
    status = request.status.toRequestStatus(),
    createdAt = request.createdAt,
)

fun MessageEntity.toDomain() = ChatMessage(
    id = id,
    requestId = requestId,
    senderEmail = senderEmail,
    senderName = senderName,
    text = text,
    sentAt = sentAt,
)

fun ContactEntity.toDomain() = Contact(
    email = contactEmail,
    name = contactName,
    affiliation = affiliationOf(contactRole, contactCourse),
    addedAt = addedAt,
)

fun String.toRequestStatus(): RequestStatus =
    RequestStatus.entries.firstOrNull { it.name == this } ?: RequestStatus.PENDING

fun RideEntity.toDto() = RideDto(
    id = remoteId?.let { JsonPrimitive(it) },
    rideId = id,
    driverEmail = driverEmail,
    driverName = driverName,
    origin = origin,
    destination = destination,
    route = route,
    departureAt = departureAt,
    seatsTotal = seatsTotal,
    vehicle = vehicle,
    notes = notes,
    driverRole = driverRole,
    driverCourse = driverCourse,
)

fun RideDto.toEntity(): RideEntity {
    val serverId = id?.content
    return RideEntity(
        id = rideId ?: "remote-$serverId",
        remoteId = serverId,
        driverEmail = driverEmail,
        driverName = driverName,
        origin = origin,
        destination = destination,
        route = route,
        departureAt = departureAt,
        seatsTotal = seatsTotal,
        vehicle = vehicle,
        notes = notes,
        synced = true,
        driverRole = driverRole,
        driverCourse = driverCourse,
    )
}
