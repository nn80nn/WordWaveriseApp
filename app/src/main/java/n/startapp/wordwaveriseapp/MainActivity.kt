package n.startapp.wordwaveriseapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import n.startapp.wordwaveriseapp.presentation.auth.AuthScreen
import n.startapp.wordwaveriseapp.presentation.auth.AuthViewModel
import n.startapp.wordwaveriseapp.presentation.detail.WordDetailScreen
import n.startapp.wordwaveriseapp.presentation.detail.WordDetailViewModel
import n.startapp.wordwaveriseapp.presentation.navigation.BottomNavigationBar
import n.startapp.wordwaveriseapp.presentation.navigation.Screen
import n.startapp.wordwaveriseapp.presentation.profile.ProfileScreen
import n.startapp.wordwaveriseapp.presentation.saved.SavedScreen
import n.startapp.wordwaveriseapp.presentation.saved.SavedWordsViewModel
import n.startapp.wordwaveriseapp.presentation.search.SearchScreen
import n.startapp.wordwaveriseapp.presentation.search.SearchViewModel
import n.startapp.wordwaveriseapp.presentation.tasks.TasksScreen
import n.startapp.wordwaveriseapp.ui.theme.WordWaveriseAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordWaveriseAppTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.state

                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val showBottomBar = authState.isLoggedIn &&
                    currentRoute != Screen.WordDetail.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    if (!authState.isLoggedIn) {
                        AuthScreen(
                            state = authState,
                            onEmailChange = authViewModel::onEmailChange,
                            onPasswordChange = authViewModel::onPasswordChange,
                            onLogin = authViewModel::login,
                            onRegister = authViewModel::register,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Search.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Search.route) {
                                val viewModel: SearchViewModel = hiltViewModel()
                                SearchScreen(
                                    state = viewModel.state.value,
                                    isSaved = viewModel.isSaved.value,
                                    onSearchQueryChange = viewModel::onSearchQueryChange,
                                    onSearch = viewModel::searchWord,
                                    onClear = viewModel::clearSearch,
                                    onSaveWord = viewModel::saveWord,
                                    onUnsaveWord = viewModel::unsaveWord,
                                    onWordClick = { word ->
                                        navController.navigate(Screen.WordDetail.createRoute(word))
                                    }
                                )
                            }

                            composable(Screen.Saved.route) {
                                val viewModel: SavedWordsViewModel = hiltViewModel()
                                SavedScreen(
                                    state = viewModel.state.value,
                                    onDeleteWord = viewModel::deleteWord,
                                    onWordClick = { word ->
                                        navController.navigate(Screen.WordDetail.createRoute(word))
                                    }
                                )
                            }

                            composable(Screen.Tasks.route) {
                                TasksScreen()
                            }

                            composable(Screen.Profile.route) {
                                ProfileScreen(
                                    userEmail = authState.userEmail ?: "",
                                    onLogout = {
                                        authViewModel.logout()
                                    }
                                )
                            }

                            composable(
                                route = Screen.WordDetail.route,
                                arguments = listOf(
                                    navArgument("word") { type = NavType.StringType }
                                )
                            ) {
                                val viewModel: WordDetailViewModel = hiltViewModel()
                                val state by viewModel.state.collectAsState()
                                WordDetailScreen(
                                    wordDetail = state.wordDetail,
                                    isLoading = state.isLoading,
                                    error = state.error,
                                    isSaved = state.isSaved,
                                    onSaveWord = viewModel::saveWord,
                                    onUnsaveWord = viewModel::unsaveWord
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
