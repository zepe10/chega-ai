package br.com.habil.carona

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.habil.carona.ui.AppViewModel
import br.com.habil.carona.ui.SessionState
import br.com.habil.carona.ui.ViewModelFactories
import br.com.habil.carona.ui.components.LoadingState
import br.com.habil.carona.ui.navigation.AuthNavHost
import br.com.habil.carona.ui.navigation.MainScaffold
import br.com.habil.carona.ui.theme.CaronaTheme
import br.com.habil.carona.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factories = ViewModelFactories((application as CaronaApplication).container)

        setContent {
            val appViewModel: AppViewModel = viewModel(factory = factories.app)
            val appState by appViewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = appState.themeMode.isDark()

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }

            CaronaTheme(darkTheme = darkTheme) {
                when (appState.session) {
                    SessionState.Loading -> LoadingState()
                    SessionState.LoggedOut -> AuthNavHost(factories)
                    is SessionState.LoggedIn -> MainScaffold(factories)
                }
            }
        }
    }
}
