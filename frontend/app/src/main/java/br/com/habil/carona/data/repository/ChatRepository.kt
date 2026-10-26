package br.com.habil.carona.data.repository

import br.com.habil.carona.data.local.dao.MessageDao
import br.com.habil.carona.data.local.dao.RideDao
import br.com.habil.carona.data.local.dao.RideRequestDao
import br.com.habil.carona.data.local.entity.MessageEntity
import br.com.habil.carona.data.mapper.toDomain
import br.com.habil.carona.data.mapper.toRequestStatus
import br.com.habil.carona.data.remote.runCatchingSuspend
import br.com.habil.carona.domain.model.ChatMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(
    private val messageDao: MessageDao,
    private val requestDao: RideRequestDao,
    private val rideDao: RideDao,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeMessages(requestId: Long): Flow<List<ChatMessage>> =
        messageDao.observeByRequest(requestId).map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    suspend fun send(requestId: Long, text: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            val request = requestDao.getById(requestId) ?: error("Conversa não encontrada.")
            val ride = rideDao.getById(request.rideId) ?: error("Carona não encontrada.")
            check(user.email == request.passengerEmail || user.email == ride.driverEmail.lowercase()) {
                "Você não participa desta conversa."
            }
            check(request.status.toRequestStatus().isActive) { "A solicitação foi encerrada; o chat está fechado." }
            messageDao.insert(
                MessageEntity(
                    requestId = requestId,
                    senderEmail = user.email,
                    senderName = user.name,
                    text = text.trim(),
                    sentAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
