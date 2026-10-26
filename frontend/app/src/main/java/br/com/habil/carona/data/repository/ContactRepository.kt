package br.com.habil.carona.data.repository

import br.com.habil.carona.data.local.dao.ContactDao
import br.com.habil.carona.data.local.dao.UserDao
import br.com.habil.carona.data.local.entity.ContactEntity
import br.com.habil.carona.data.mapper.toDomain
import br.com.habil.carona.data.remote.runCatchingSuspend
import br.com.habil.carona.domain.model.Affiliation
import br.com.habil.carona.domain.model.Contact
import br.com.habil.carona.domain.validation.EmailPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ContactRepository(
    private val contactDao: ContactDao,
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeContacts(): Flow<List<Contact>> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            contactDao.observeByOwner(user.email).map { rows -> rows.map { it.toDomain() } }
        }
    }.flowOn(ioDispatcher)

    suspend fun addContact(
        email: String,
        knownName: String? = null,
        knownAffiliation: Affiliation = Affiliation(),
    ): Result<Contact> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val me = authRepository.requireUser()
            EmailPolicy.validate(email)?.let { error(it) }
            val normalized = EmailPolicy.normalize(email)
            check(normalized != me.email) { "Você não pode adicionar a si mesmo." }
            val registered = userDao.findByEmail(normalized)
            val name = registered?.name ?: knownName ?: error("Nenhum usuário cadastrado com este e-mail.")
            val affiliation = registered?.toDomain()?.affiliation ?: knownAffiliation
            check(contactDao.count(me.email, normalized) == 0) { "$name já está nos seus contatos." }
            val now = System.currentTimeMillis()
            contactDao.insert(
                ContactEntity(
                    ownerEmail = me.email,
                    contactEmail = normalized,
                    contactName = name,
                    addedAt = now,
                    contactRole = affiliation.role?.name.orEmpty(),
                    contactCourse = affiliation.course,
                ),
            )
            Contact(email = normalized, name = name, affiliation = affiliation, addedAt = now)
        }
    }

    suspend fun removeContact(email: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val me = authRepository.requireUser()
            contactDao.delete(me.email, EmailPolicy.normalize(email))
        }
    }
}
