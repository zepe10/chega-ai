package br.com.habil.carona.data.repository

import br.com.habil.carona.data.local.dao.RideDao
import br.com.habil.carona.data.local.entity.RideEntity
import br.com.habil.carona.data.mapper.toDomain
import br.com.habil.carona.data.mapper.toDto
import br.com.habil.carona.data.mapper.toEntity
import br.com.habil.carona.data.remote.CaronaApi
import br.com.habil.carona.data.remote.runCatchingSuspend
import br.com.habil.carona.domain.model.Ride
import br.com.habil.carona.domain.model.RideDraft
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RideRepository(
    private val rideDao: RideDao,
    private val api: CaronaApi,
    private val authRepository: AuthRepository,
    private val syncScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeRides(): Flow<List<Ride>> =
        rideDao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    fun observeRide(id: String): Flow<Ride?> =
        rideDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMyRides(): Flow<List<Ride>> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            rideDao.observeByDriver(user.email).map { rows -> rows.map { it.toDomain() } }
        }
    }.flowOn(ioDispatcher)

    suspend fun getRide(id: String): Ride? = observeRide(id).first()

    suspend fun refresh(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            pushPending()
            val remote = api.getRides().map { it.toEntity() }
            val pendingIds = rideDao.getUnsynced().map { it.id }.toSet()
            rideDao.applyRemote(remote.filterNot { it.id in pendingIds })
        }
    }

    suspend fun saveRide(draft: RideDraft, editingId: String?): Result<String> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            val entity = if (editingId == null) {
                RideEntity(
                    id = UUID.randomUUID().toString(),
                    remoteId = null,
                    driverEmail = user.email,
                    driverName = user.name,
                    origin = draft.origin.trim(),
                    destination = draft.destination.trim(),
                    route = draft.route.trim(),
                    departureAt = draft.departureAt,
                    seatsTotal = draft.seatsTotal,
                    vehicle = draft.vehicle.trim(),
                    notes = draft.notes.trim(),
                    synced = false,
                    driverRole = user.affiliation.role?.name.orEmpty(),
                    driverCourse = user.affiliation.course,
                )
            } else {
                val current = rideDao.getById(editingId) ?: error("Carona não encontrada.")
                check(current.driverEmail.lowercase() == user.email) { "Apenas o motorista pode editar a carona." }
                val seatsTaken = rideDao.observeById(editingId).first()?.seatsTaken ?: 0
                check(draft.seatsTotal >= seatsTaken) {
                    "Já existem $seatsTaken passageiro(s) confirmado(s); o total de vagas não pode ser menor."
                }
                current.copy(
                    origin = draft.origin.trim(),
                    destination = draft.destination.trim(),
                    route = draft.route.trim(),
                    departureAt = draft.departureAt,
                    seatsTotal = draft.seatsTotal,
                    vehicle = draft.vehicle.trim(),
                    notes = draft.notes.trim(),
                    synced = false,
                )
            }
            rideDao.upsert(entity)
            syncScope.launch { runCatchingSuspend { pushPending() } }
            entity.id
        }
    }

    suspend fun deleteRide(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            val ride = rideDao.getById(id)
            if (ride != null) {
                check(ride.driverEmail.lowercase() == user.email) { "Apenas o motorista pode excluir a carona." }
                val remoteId = ride.remoteId
                if (remoteId != null) {
                    val response = api.deleteRide(remoteId)
                    if (!response.isSuccessful && response.code() != 404) throw HttpException(response)
                }
                rideDao.deleteById(id)
            }
            Unit
        }
    }

    private suspend fun pushPending() {
        rideDao.getUnsynced().forEach { ride ->
            runCatchingSuspend {
                val remoteId = ride.remoteId
                val serverId = if (remoteId == null) {
                    api.createRide(ride.toDto()).id?.content ?: ride.id
                } else {
                    api.updateRide(remoteId, ride.toDto())
                    remoteId
                }
                rideDao.markSynced(ride.id, serverId)
            }
        }
    }
}
