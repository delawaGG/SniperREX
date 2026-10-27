package com.sniperrex.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.sniperrex.app.presentation.dashboard.DashboardScreen
import com.sniperrex.app.presentation.pair.PairScreen
import com.sniperrex.app.presentation.settings.SettingsScreen
import com.sniperrex.app.presentation.signals.SignalDetailScreen
import com.sniperrex.app.presentation.signals.SignalsScreen
import com.sniperrex.app.presentation.strategy.StrategyScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val PAIR = "pair"
    const val STRATEGY = "strategy"
    const val SIGNALS = "signals"
    const val DETAIL = "detail/{signalId}"
    fun detail(id: String) = "detail/$id"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSignals = { navController.navigate(Routes.SIGNALS) },
                onScan = { pair -> navController.navigate("${Routes.STRATEGY}?pair=$pair&tf=M15") },
                onOpenSignal = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PAIR) {
            PairScreen(
                onBack = { navController.popBackStack() },
                onContinue = { pair, tf -> navController.navigate("${Routes.STRATEGY}?pair=$pair&tf=$tf") },
            )
        }
        composable(
            "${Routes.STRATEGY}?pair={pair}&tf={tf}",
            arguments = listOf(
                navArgument("pair") { type = NavType.StringType; defaultValue = "XAUUSD" },
                navArgument("tf") { type = NavType.StringType; defaultValue = "M15" },
            ),
        ) { entry ->
            StrategyScreen(
                pair = entry.arguments?.getString("pair") ?: "XAUUSD",
                timeframe = entry.arguments?.getString("tf") ?: "M15",
                onBack = { navController.popBackStack() },
                onScanned = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.SIGNALS) {
            SignalsScreen(
                onBack = { navController.popBackStack() },
                onOpenSignal = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("signalId") { type = NavType.StringType })) { entry ->
            SignalDetailScreen(
                signalId = entry.arguments?.getString("signalId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
