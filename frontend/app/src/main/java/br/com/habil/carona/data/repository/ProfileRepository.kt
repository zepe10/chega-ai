package br.com.habil.carona.data.repository

import br.com.habil.carona.data.local.ProfilePhotoStorage
import br.com.habil.carona.data.local.dao.UserDao
import br.com.habil.carona.data.remote.runCatchingSuspend
import br.com.habil.carona.domain.model.UserRole
import br.com.habil.carona.domain.validation.PasswordPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val photoStorage: ProfilePhotoStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun updateProfile(name: String, role: UserRole, course: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingSuspend {
                val user = authRepository.requireUser()
                check(name.trim().length >= 3) { "Informe seu nome completo." }
                check(role != UserRole.STUDENT || course.isNotBlank()) { "Informe o curso do estudante." }
                userDao.updateProfile(
                    email = user.email,
                    name = name.trim(),
                    role = role.name,
                    course = if (role == UserRole.STUDENT) course.trim() else "",
                )
            }
        }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingSuspend {
                val user = authRepository.requireUser()
                val stored = userDao.findByEmail(user.email) ?: error("Conta não encontrada.")
                check(PasswordHasher.hash(currentPassword, stored.salt) == stored.passwordHash) {
                    "A senha atual está incorreta."
                }
                PasswordPolicy.validate(newPassword)?.let { error(it) }
                check(newPassword != currentPassword) { "A nova senha deve ser diferente da atual." }
                val salt = PasswordHasher.newSalt()
                userDao.updateCredentials(user.email, salt, PasswordHasher.hash(newPassword, salt))
            }
        }

    suspend fun setPhoto(sourceUri: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            val newPath = photoStorage.save(sourceUri, user.email)
            userDao.updatePhoto(user.email, newPath)
            photoStorage.delete(user.photoPath)
        }
    }

    suspend fun removePhoto(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingSuspend {
            val user = authRepository.requireUser()
            userDao.updatePhoto(user.email, "")
            photoStorage.delete(user.photoPath)
        }
    }
}
