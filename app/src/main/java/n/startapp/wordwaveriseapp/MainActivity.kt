package n.startapp.wordwaveriseapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import n.startapp.wordwaveriseapp.presentation.navigation.BottomNavigationBar
import n.startapp.wordwaveriseapp.presentation.navigation.Screen
import n.startapp.wordwaveriseapp.presentation.profile.ProfileScreen
import n.startapp.wordwaveriseapp.presentation.saved.SavedScreen
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
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigationBar(navController = navController)
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Search.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Search.route) {
                            val viewModel: SearchViewModel = hiltViewModel()
                            SearchScreen(
                                state = viewModel.state.value,
                                onSearchQueryChange = viewModel::onSearchQueryChange,
                                onSearch = viewModel::searchWord,
                                onClear = viewModel::clearSearch
                            )
                        }

                        composable(Screen.Saved.route) {
                            SavedScreen()
                        }

                        composable(Screen.Tasks.route) {
                            TasksScreen()
                        }

                        composable(Screen.Profile.route) {
                            ProfileScreen()
                        }
                    }
                }
            }
        }
    }
}