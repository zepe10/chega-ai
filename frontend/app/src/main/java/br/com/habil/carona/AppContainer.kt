package br.com.habil.carona

import android.content.Context
import br.com.habil.carona.data.local.CaronaDatabase
import br.com.habil.carona.data.local.ProfilePhotoStorage
import br.com.habil.carona.data.local.UserPreferencesDataSource
import br.com.habil.carona.data.local.userPrefsDataStore
import br.com.habil.carona.data.remote.NetworkModule
import br.com.habil.carona.data.repository.AuthRepository
import br.com.habil.carona.data.repository.ChatRepository
import br.com.habil.carona.data.repository.ContactRepository
import br.com.habil.carona.data.repository.ProfileRepository
import br.com.habil.carona.data.repository.RequestRepository
import br.com.habil.carona.data.repository.RideRepository
import br.com.habil.carona.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = CaronaDatabase.create(appContext)
    private val preferences = UserPreferencesDataSource(appContext.userPrefsDataStore)
    private val api = NetworkModule.createApi(BuildConfig.API_BASE_URL, BuildConfig.DEBUG)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val authRepository = AuthRepository(database.userDao(), preferences)
    val settingsRepository = SettingsRepository(preferences)
    val rideRepository = RideRepository(database.rideDao(), api, authRepository, syncScope)
    val requestRepository = RequestRepository(
        database = database,
        rideDao = database.rideDao(),
        requestDao = database.rideRequestDao(),
        authRepository = authRepository,
    )
    val chatRepository = ChatRepository(
        messageDao = database.messageDao(),
        requestDao = database.rideRequestDao(),
        rideDao = database.rideDao(),
        authRepository = authRepository,
    )
    val contactRepository = ContactRepository(
        contactDao = database.contactDao(),
        userDao = database.userDao(),
        authRepository = authRepository,
    )
    val profileRepository = ProfileRepository(
        userDao = database.userDao(),
        authRepository = authRepository,
        photoStorage = ProfilePhotoStorage(appContext),
    )
}
