package br.com.habil.carona.data.repository

import androidx.room.withTransaction
import br.com.habil.carona.data.local.CaronaDatabase
import br.com.habil.carona.data.local.dao.RideDao
import br.com.habil.carona.data.local.dao.RideRequestDao
import br.com.habil.carona.data.local.entity.RideRequestEntity
import br.com.habil.carona.data.mapper.toDomain
import br.com.habil.carona.data.mapper.toRequestStatus
import br.com.habil.carona.data.remote.runCatchingSuspend
import br.com.habil.carona.domain.model.RequestStatus
import br.com.habil.carona.domain.model.RideRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RequestRepository(
    private val database: CaronaDatabase,
    private val rideDao: RideDao,
    private val requestDao: RideRequestDao,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMyRequests(): Flow<List<RideRequest>> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            requestDao.observeByPassenger(user.email).map { rows -> rows.map { it.toDomain() } }
        }
    }.flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeReceivedRequests(): Flow<List<RideRequest>> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            requestDao.observeByDriver(user.email).map { rows -> rows.map { it.toDomain() } }
        }
    }.flowOn(ioDispatcher)

    fun observeForRide(rideId: String): Flow<List<RideRequest>> =
        requestDao.observeByRide(rideId).map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    fun observeById(id: Long): Flow<RideRequest?> =
        requestDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

    suspend fun requestSeat(rideId: String): Result<Long> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            database.withTransaction {
                val ride = rideDao.getById(rideId) ?: error("Carona não encontrada.")
                check(ride.driverEmail.lowercase() != user.email) { "Você não pode solicitar vaga na sua própria carona." }
                check(ride.departureAt > System.currentTimeMillis()) { "Esta carona já partiu." }
                check(requestDao.activeCount(rideId, user.email) == 0) {
                    "Você já possui uma solicitação ativa para esta carona."
                }
                check(requestDao.confirmedCount(rideId) < ride.seatsTotal) { "Não há vagas disponíveis." }
                requestDao.insert(
                    RideRequestEntity(
                        rideId = rideId,
                        passengerEmail = user.email,
                        passengerName = user.name,
                        status = RequestStatus.PENDING.name,
                        createdAt = System.currentTimeMillis(),
                        passengerRole = user.affiliation.role?.name.orEmpty(),
                        passengerCourse = user.affiliation.course,
                    ),
                )
            }
        }
    }

    suspend fun confirm(requestId: Long): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            database.withTransaction {
                val request = requestDao.getById(requestId) ?: error("Solicitação não encontrada.")
                val ride = rideDao.getById(request.rideId) ?: error("Carona não encontrada.")
                check(ride.driverEmail.lowercase() == user.email) { "Apenas o motorista pode confirmar solicitações." }
                check(request.status.toRequestStatus() == RequestStatus.PENDING) { "A solicitação já foi respondida." }
                check(requestDao.confirmedCount(ride.id) < ride.seatsTotal) { "Todas as vagas já foram confirmadas." }
                requestDao.updateStatus(requestId, RequestStatus.CONFIRMED.name)
            }
        }
    }

    suspend fun reject(requestId: Long): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            database.withTransaction {
                val request = requestDao.getById(requestId) ?: error("Solicitação não encontrada.")
                val ride = rideDao.getById(request.rideId) ?: error("Carona não encontrada.")
                check(ride.driverEmail.lowercase() == user.email) { "Apenas o motorista pode recusar solicitações." }
                check(request.status.toRequestStatus().isActive) { "A solicitação já foi encerrada." }
                requestDao.updateStatus(requestId, RequestStatus.REJECTED.name)
            }
        }
    }

    suspend fun cancel(requestId: Long): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            database.withTransaction {
                val request = requestDao.getById(requestId) ?: error("Solicitação não encontrada.")
                check(request.passengerEmail == user.email) { "Apenas o passageiro pode cancelar a solicitação." }
                check(request.status.toRequestStatus().isActive) { "A solicitação já foi encerrada." }
                requestDao.updateStatus(requestId, RequestStatus.CANCELED.name)
            }
        }
    }

    suspend fun clearHistory(): Result<Int> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            requestDao.clearHistory(user.email, System.currentTimeMillis())
        }
    }
}
