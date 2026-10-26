package br.com.habil.carona.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import br.com.habil.carona.data.local.entity.ContactEntity
import br.com.habil.carona.data.local.entity.MessageEntity
import br.com.habil.carona.data.local.entity.RequestWithRide
import br.com.habil.carona.data.local.entity.RideEntity
import br.com.habil.carona.data.local.entity.RideRequestEntity
import br.com.habil.carona.data.local.entity.RideWithSeats
import br.com.habil.carona.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

private const val SEATS_TAKEN_SQL =
    "(SELECT COUNT(*) FROM ride_requests q WHERE q.rideId = r.id AND q.status = 'CONFIRMED')"

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    fun observeByEmail(email: String): Flow<UserEntity?>

    @Insert
    suspend fun insert(user: UserEntity)

    @Query("UPDATE users SET name = :name, role = :role, course = :course WHERE email = :email")
    suspend fun updateProfile(email: String, name: String, role: String, course: String)

    @Query("UPDATE users SET salt = :salt, passwordHash = :passwordHash WHERE email = :email")
    suspend fun updateCredentials(email: String, salt: String, passwordHash: String)

    @Query("UPDATE users SET photoPath = :photoPath WHERE email = :email")
    suspend fun updatePhoto(email: String, photoPath: String)
}

@Dao
abstract class RideDao {
    @Query("SELECT r.*, $SEATS_TAKEN_SQL AS seatsTaken FROM rides r ORDER BY r.departureAt ASC")
    abstract fun observeAll(): Flow<List<RideWithSeats>>

    @Query("SELECT r.*, $SEATS_TAKEN_SQL AS seatsTaken FROM rides r WHERE r.id = :id")
    abstract fun observeById(id: String): Flow<RideWithSeats?>

    @Query(
        "SELECT r.*, $SEATS_TAKEN_SQL AS seatsTaken FROM rides r " +
            "WHERE r.driverEmail = :email ORDER BY r.departureAt ASC",
    )
    abstract fun observeByDriver(email: String): Flow<List<RideWithSeats>>

    @Query("SELECT * FROM rides WHERE id = :id")
    abstract suspend fun getById(id: String): RideEntity?

    @Query("SELECT * FROM rides WHERE synced = 0")
    abstract suspend fun getUnsynced(): List<RideEntity>

    @Upsert
    abstract suspend fun upsert(ride: RideEntity)

    @Upsert
    abstract suspend fun upsertAll(rides: List<RideEntity>)

    @Query("UPDATE rides SET synced = 1, remoteId = :remoteId WHERE id = :id")
    abstract suspend fun markSynced(id: String, remoteId: String)

    @Query("DELETE FROM rides WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    @Query("DELETE FROM rides WHERE synced = 1 AND id NOT IN (:keepIds)")
    abstract suspend fun deleteSyncedNotIn(keepIds: List<String>)

    @Transaction
    open suspend fun applyRemote(remote: List<RideEntity>) {
        deleteSyncedNotIn(remote.map { it.id })
        upsertAll(remote)
    }
}

@Dao
interface RideRequestDao {
    @Transaction
    @Query("SELECT * FROM ride_requests WHERE passengerEmail = :email ORDER BY createdAt DESC")
    fun observeByPassenger(email: String): Flow<List<RequestWithRide>>

    @Transaction
    @Query(
        "SELECT q.* FROM ride_requests q INNER JOIN rides r ON r.id = q.rideId " +
            "WHERE r.driverEmail = :email ORDER BY q.createdAt DESC",
    )
    fun observeByDriver(email: String): Flow<List<RequestWithRide>>

    @Transaction
    @Query("SELECT * FROM ride_requests WHERE rideId = :rideId ORDER BY createdAt ASC")
    fun observeByRide(rideId: String): Flow<List<RequestWithRide>>

    @Transaction
    @Query("SELECT * FROM ride_requests WHERE id = :id")
    fun observeById(id: Long): Flow<RequestWithRide?>

    @Query("SELECT * FROM ride_requests WHERE id = :id")
    suspend fun getById(id: Long): RideRequestEntity?

    @Insert
    suspend fun insert(request: RideRequestEntity): Long

    @Query("UPDATE ride_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM ride_requests WHERE rideId = :rideId AND status = 'CONFIRMED'")
    suspend fun confirmedCount(rideId: String): Int

    @Query(
        "SELECT COUNT(*) FROM ride_requests " +
            "WHERE rideId = :rideId AND passengerEmail = :email AND status IN ('PENDING', 'CONFIRMED')",
    )
    suspend fun activeCount(rideId: String, email: String): Int

    @Query(
        "DELETE FROM ride_requests WHERE passengerEmail = :email AND " +
            "(status IN ('REJECTED', 'CANCELED') OR rideId IN (SELECT id FROM rides WHERE departureAt < :now))",
    )
    suspend fun clearHistory(email: String, now: Long): Int
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE ownerEmail = :owner ORDER BY contactName COLLATE NOCASE ASC")
    fun observeByOwner(owner: String): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM contacts WHERE ownerEmail = :owner AND contactEmail = :contact")
    suspend fun count(owner: String, contact: String): Int

    @Insert
    suspend fun insert(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE ownerEmail = :owner AND contactEmail = :contact")
    suspend fun delete(owner: String, contact: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE requestId = :requestId ORDER BY sentAt ASC, id ASC")
    fun observeByRequest(requestId: Long): Flow<List<MessageEntity>>

    @Insert
    suspend fun insert(message: MessageEntity)
}
