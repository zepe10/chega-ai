package br.com.habil.carona.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.habil.carona.AppContainer
import br.com.habil.carona.ui.activity.ActivityViewModel
import br.com.habil.carona.ui.auth.LoginViewModel
import br.com.habil.carona.ui.auth.RegisterViewModel
import br.com.habil.carona.ui.chat.ChatViewModel
import br.com.habil.carona.ui.contacts.ContactsViewModel
import br.com.habil.carona.ui.profile.ProfileViewModel
import br.com.habil.carona.ui.rides.RideDetailViewModel
import br.com.habil.carona.ui.rides.RideFormViewModel
import br.com.habil.carona.ui.rides.RideListViewModel

class ViewModelFactories(container: AppContainer) {
    val app: ViewModelProvider.Factory = viewModelFactory {
        initializer { AppViewModel(container.authRepository, container.settingsRepository) }
    }

    val login: ViewModelProvider.Factory = viewModelFactory {
        initializer { LoginViewModel(container.authRepository) }
    }

    val register: ViewModelProvider.Factory = viewModelFactory {
        initializer { RegisterViewModel(container.authRepository) }
    }

    val rideList: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            RideListViewModel(
                container.rideRepository,
                container.settingsRepository,
                container.authRepository,
                container.contactRepository,
            )
        }
    }

    val rideDetail: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            RideDetailViewModel(
                createSavedStateHandle(),
                container.rideRepository,
                container.requestRepository,
                container.authRepository,
                container.contactRepository,
            )
        }
    }

    val rideForm: ViewModelProvider.Factory = viewModelFactory {
        initializer { RideFormViewModel(createSavedStateHandle(), container.rideRepository, container.authRepository) }
    }

    val activity: ViewModelProvider.Factory = viewModelFactory {
        initializer { ActivityViewModel(container.rideRepository, container.requestRepository) }
    }

    val chat: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ChatViewModel(
                createSavedStateHandle(),
                container.chatRepository,
                container.requestRepository,
                container.authRepository,
            )
        }
    }

    val contacts: ViewModelProvider.Factory = viewModelFactory {
        initializer { ContactsViewModel(container.contactRepository) }
    }

    val profile: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ProfileViewModel(
                container.authRepository,
                container.settingsRepository,
                container.profileRepository,
                container.contactRepository,
            )
        }
    }
}
