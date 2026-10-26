package br.com.habil.carona.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.habil.carona.ui.ViewModelFactories
import br.com.habil.carona.ui.activity.ActivityRoute
import br.com.habil.carona.ui.chat.ChatRoute
import br.com.habil.carona.ui.contacts.ContactsRoute
import br.com.habil.carona.ui.profile.ProfileRoute
import br.com.habil.carona.ui.rides.RideDetailRoute
import br.com.habil.carona.ui.rides.RideFormRoute
import br.com.habil.carona.ui.rides.RideListRoute

private data class TopLevelDestination(
    val graphRoute: String,
    val startRoute: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.RIDES_GRAPH, Routes.RIDE_LIST, "Caronas", Icons.Default.DirectionsCar),
    TopLevelDestination(Routes.ACTIVITY_GRAPH, Routes.ACTIVITY, "Atividade", Icons.Default.Dashboard),
    TopLevelDestination(Routes.CONTACTS_GRAPH, Routes.CONTACTS, "Contatos", Icons.Default.People),
    TopLevelDestination(Routes.PROFILE_GRAPH, Routes.PROFILE, "Perfil", Icons.Default.Person),
)

@Composable
fun MainScaffold(factories: ViewModelFactories) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = topLevelDestinations.any { it.startRoute == currentDestination?.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.graphRoute } == true,
                            onClick = { navController.navigateToTopLevel(destination.graphRoute) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        MainNavHost(
            navController = navController,
            factories = factories,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

private fun NavHostController.navigateToTopLevel(graphRoute: String) {
    navigate(graphRoute) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    factories: ViewModelFactories,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = Routes.RIDES_GRAPH, modifier = modifier) {

        navigation(startDestination = Routes.RIDE_LIST, route = Routes.RIDES_GRAPH) {
            composable(Routes.RIDE_LIST) {
                RideListRoute(
                    viewModel = viewModel(factory = factories.rideList),
                    onRideClick = { navController.navigate(Routes.rideDetail(it)) },
                    onCreateRide = { navController.navigate(Routes.rideForm()) },
                )
            }
            composable(
                route = Routes.RIDE_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_RIDE_ID) { type = NavType.StringType }),
            ) {
                RideDetailRoute(
                    viewModel = viewModel(factory = factories.rideDetail),
                    onBack = { navController.popBackStack() },
                    onEditRide = { navController.navigate(Routes.rideForm(it)) },
                    onOpenChat = { navController.navigate(Routes.chat(it)) },
                )
            }
            composable(
                route = Routes.RIDE_FORM,
                arguments = listOf(
                    navArgument(Routes.ARG_RIDE_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                RideFormRoute(
                    viewModel = viewModel(factory = factories.rideForm),
                    onClose = { navController.popBackStack() },
                )
            }
        }

        navigation(startDestination = Routes.ACTIVITY, route = Routes.ACTIVITY_GRAPH) {
            composable(Routes.ACTIVITY) {
                ActivityRoute(
                    viewModel = viewModel(factory = factories.activity),
                    onRideClick = { navController.navigate(Routes.rideDetail(it)) },
                    onOpenChat = { navController.navigate(Routes.chat(it)) },
                    onCreateRide = { navController.navigate(Routes.rideForm()) },
                )
            }
            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument(Routes.ARG_REQUEST_ID) { type = NavType.LongType }),
            ) {
                ChatRoute(
                    viewModel = viewModel(factory = factories.chat),
                    onBack = { navController.popBackStack() },
                )
            }
        }

        navigation(startDestination = Routes.CONTACTS, route = Routes.CONTACTS_GRAPH) {
            composable(Routes.CONTACTS) {
                ContactsRoute(viewModel = viewModel(factory = factories.contacts))
            }
        }

        navigation(startDestination = Routes.PROFILE, route = Routes.PROFILE_GRAPH) {
            composable(Routes.PROFILE) {
                ProfileRoute(
                    viewModel = viewModel(factory = factories.profile),
                    onOpenContacts = { navController.navigateToTopLevel(Routes.CONTACTS_GRAPH) },
                )
            }
        }
    }
}
