package com.example

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.CaseRepository
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainScreen

@Composable
fun AppNavigation(repository: CaseRepository) {
    val navController = rememberNavController()
    val startDestination = if (repository.hasSession()) "main" else "login"
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController, repository) }
        composable("main") { MainScreen(navController, repository) }
    }
}
