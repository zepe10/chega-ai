package br.com.habil.carona.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.habil.carona.ui.ViewModelFactories
import br.com.habil.carona.ui.auth.LoginRoute
import br.com.habil.carona.ui.auth.RegisterRoute

@Composable
fun AuthNavHost(factories: ViewModelFactories) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginRoute(
                viewModel = viewModel(factory = factories.login),
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterRoute(
                viewModel = viewModel(factory = factories.register),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
