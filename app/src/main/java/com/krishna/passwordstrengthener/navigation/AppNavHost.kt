package com.krishna.passwordstrengthener.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.krishna.passwordstrengthener.model.ModelRepository
import com.krishna.passwordstrengthener.ui.DashboardScreen
import com.krishna.passwordstrengthener.ui.ModelPickerScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = remember {
        mutableStateOf(
            if (ModelRepository.getModelPath(context).isNullOrBlank()) NavRoutes.ModelPicker else NavRoutes.Dashboard
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination.value
    ) {
        composable(NavRoutes.ModelPicker) {
            ModelPickerScreen(navController)
        }
        composable(NavRoutes.Dashboard) {
            DashboardScreen(navController)
        }
    }
}
