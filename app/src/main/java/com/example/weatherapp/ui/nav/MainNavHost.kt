package com.example.weatherapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.weatherapp.ui.HomePage
import com.example.weatherapp.ui.ListPage
import com.example.weatherapp.ui.MapPage
import com.example.weatherapp.viewmodel.MainViewModel

@Composable
fun MainNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {

    NavHost(
        navController = navController,
        startDestination = Route.Home
    ) {

        composable<Route.Home> {
            HomePage(
                modifier = modifier,
                viewModel = viewModel
            )
        }

        composable<Route.List> {
            ListPage(
                modifier = modifier,
                viewModel = viewModel
            )
        }

        composable<Route.Map> {
            MapPage(
                modifier = modifier,
                viewModel = viewModel
            )
        }
    }
}