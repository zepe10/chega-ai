package br.com.habil.carona.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val salt: String,
    val passwordHash: String,
    val role: String = "",
    val course: String = "",
    val photoPath: String = "",
)

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val remoteId: String?,
    val driverEmail: String,
    val driverName: String,
    val origin: String,
    val destination: String,
    val route: String,
    val departureAt: Long,
    val seatsTotal: Int,
    val vehicle: String,
    val notes: String,
    val synced: Boolean,
    val driverRole: String = "",
    val driverCourse: String = "",
)

@Entity(
    tableName = "ride_requests",
    foreignKeys = [
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("rideId"), Index("passengerEmail")],
)
data class RideRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: String,
    val passengerEmail: String,
    val passengerName: String,
    val status: String,
    val createdAt: Long,
    val passengerRole: String = "",
    val passengerCourse: String = "",
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = RideRequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["requestId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("requestId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: Long,
    val senderEmail: String,
    val senderName: String,
    val text: String,
    val sentAt: Long,
)

@Entity(
    tableName = "contacts",
    primaryKeys = ["ownerEmail", "contactEmail"],
    indices = [Index("ownerEmail")],
)
data class ContactEntity(
    val ownerEmail: String,
    val contactEmail: String,
    val contactName: String,
    val addedAt: Long,
    val contactRole: String = "",
    val contactCourse: String = "",
)

data class RideWithSeats(
    @Embedded val ride: RideEntity,
    val seatsTaken: Int,
)

data class RequestWithRide(
    @Embedded val request: RideRequestEntity,
    @Relation(parentColumn = "rideId", entityColumn = "id")
    val ride: RideEntity,
)
