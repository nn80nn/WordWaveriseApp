package com.wordwaverise.wordwaveriseapp

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
import com.wordwaverise.wordwaveriseapp.presentation.auth.AuthScreen
import com.wordwaverise.wordwaveriseapp.presentation.auth.AuthViewModel
import com.wordwaverise.wordwaveriseapp.presentation.detail.WordDetailScreen
import com.wordwaverise.wordwaveriseapp.presentation.detail.WordDetailViewModel
import com.wordwaverise.wordwaveriseapp.presentation.navigation.BottomNavigationBar
import com.wordwaverise.wordwaveriseapp.presentation.navigation.Screen
import com.wordwaverise.wordwaveriseapp.presentation.profile.ProfileScreen
import com.wordwaverise.wordwaveriseapp.presentation.profile.ProfileViewModel
import com.wordwaverise.wordwaveriseapp.presentation.saved.SavedScreen
import com.wordwaverise.wordwaveriseapp.presentation.saved.SavedWordsViewModel
import com.wordwaverise.wordwaveriseapp.presentation.search.SearchScreen
import com.wordwaverise.wordwaveriseapp.presentation.search.SearchViewModel
import com.wordwaverise.wordwaveriseapp.presentation.tasks.TasksScreen
import com.wordwaverise.wordwaveriseapp.ui.theme.WordWaveriseAppTheme

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
                            onLoginWithGoogle = authViewModel::loginWithGoogle,
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
                                    isPlayingAudio = viewModel.state.value.isPlayingAudio,
                                    playingAudioUrl = viewModel.state.value.playingAudioUrl,
                                    onPlayAudio = viewModel::playAudio,
                                    onStopAudio = viewModel::stopAudio,
                                    onWordClick = { word ->
                                        navController.navigate(Screen.WordDetail.createRoute(word))
                                    },
                                    onSelectSuggestion = viewModel::selectSuggestion
                                )
                            }

                            composable(Screen.Saved.route) {
                                val viewModel: SavedWordsViewModel = hiltViewModel()
                                SavedScreen(
                                    state = viewModel.state.value,
                                    onDeleteWord = viewModel::deleteWord,
                                    onWordClick = { word ->
                                        navController.navigate(Screen.WordDetail.createRoute(word))
                                    },
                                    onSelectCategory = viewModel::selectCategory,
                                    onShowCategorySheet = viewModel::showCategorySheet,
                                    onHideCategorySheet = viewModel::hideCategorySheet,
                                    onSetWordToMove = viewModel::setWordToMove,
                                    onMoveWordToCategory = viewModel::moveWordToCategory,
                                    onCreateCategory = viewModel::createCategory,
                                    onDeleteCategory = viewModel::deleteCategory,
                                    onNewCategoryNameChange = viewModel::setNewCategoryName
                                )
                            }

                            composable(Screen.Tasks.route) {
                                TasksScreen()
                            }

                            composable(Screen.Profile.route) {
                                val profileViewModel: ProfileViewModel = hiltViewModel()
                                val profileState by profileViewModel.state.collectAsState()
                                ProfileScreen(
                                    userEmail = authState.userEmail ?: "",
                                    state = profileState,
                                    onLogout = { authViewModel.logout() }
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
                                    isLoadingFull = state.isLoadingFull,
                                    error = state.error,
                                    isSaved = state.isSaved,
                                    isSavedLoading = state.isSavedLoading,
                                    onSaveWord = viewModel::saveWord,
                                    onUnsaveWord = viewModel::unsaveWord,
                                    isPlayingAudio = state.isPlayingAudio,
                                    playingAudioUrl = state.playingAudioUrl,
                                    onPlayAudio = viewModel::playAudio,
                                    onStopAudio = viewModel::stopAudio,
                                    onBack = { navController.popBackStack() },
                                    aiExplanation = state.aiExplanation,
                                    isAiExplanationLoading = state.isAiExplanationLoading,
                                    aiExamples = state.aiExamples,
                                    isAiExamplesLoading = state.isAiExamplesLoading,
                                    aiError = state.aiError,
                                    onLoadAiExplanation = viewModel::loadAiExplanation,
                                    onLoadAiExamples = viewModel::loadAiExamples,
                                    onWordClick = { word ->
                                        navController.navigate(Screen.WordDetail.createRoute(word))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
