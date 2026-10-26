package br.com.habil.carona.data.repository

import br.com.habil.carona.data.local.UserPreferencesDataSource
import br.com.habil.carona.data.local.dao.UserDao
import br.com.habil.carona.data.local.entity.UserEntity
import br.com.habil.carona.data.mapper.toDomain
import br.com.habil.carona.data.remote.runCatchingSuspend
import br.com.habil.carona.domain.model.User
import br.com.habil.carona.domain.model.UserRole
import br.com.habil.carona.domain.validation.EmailPolicy
import br.com.habil.carona.domain.validation.PasswordPolicy
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AuthRepository(
    private val userDao: UserDao,
    private val preferences: UserPreferencesDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<User?> = preferences.sessionEmail.flatMapLatest { email ->
        if (email == null) flowOf(null) else userDao.observeByEmail(email).map { it?.toDomain() }
    }

    suspend fun requireUser(): User =
        currentUser.first() ?: throw IllegalStateException("Sessão expirada. Faça login novamente.")

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        course: String,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            check(role != UserRole.STUDENT || course.isNotBlank()) { "Informe o curso do estudante." }
            PasswordPolicy.validate(password)?.let { error(it) }
            val normalized = EmailPolicy.normalize(email)
            check(userDao.findByEmail(normalized) == null) { "Já existe uma conta com este e-mail." }
            val salt = PasswordHasher.newSalt()
            userDao.insert(
                UserEntity(
                    email = normalized,
                    name = name.trim(),
                    salt = salt,
                    passwordHash = PasswordHasher.hash(password, salt),
                    role = role.name,
                    course = if (role == UserRole.STUDENT) course.trim() else "",
                ),
            )
            preferences.setSessionEmail(normalized)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val normalized = EmailPolicy.normalize(email)
            val user = userDao.findByEmail(normalized)
            check(user != null && PasswordHasher.hash(password, user.salt) == user.passwordHash) {
                "E-mail ou senha incorretos."
            }
            preferences.setSessionEmail(normalized)
        }
    }

    suspend fun logout() = preferences.setSessionEmail(null)
}

object PasswordHasher {
    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    fun hash(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
