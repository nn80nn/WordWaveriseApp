package com.wordwaverise.wordwaveriseapp

import android.content.Intent
import android.os.Bundle
import javax.inject.Inject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.wordwaverise.wordwaveriseapp.presentation.groups.GroupsScreen
import com.wordwaverise.wordwaveriseapp.presentation.navigation.BottomNavigationBar
import com.wordwaverise.wordwaveriseapp.presentation.navigation.Screen
import com.wordwaverise.wordwaveriseapp.presentation.profile.ProfileScreen
import com.wordwaverise.wordwaveriseapp.presentation.profile.ProfileViewModel
import com.wordwaverise.wordwaveriseapp.presentation.reader.BooksScreen
import com.wordwaverise.wordwaveriseapp.presentation.reader.ReaderScreen
import com.wordwaverise.wordwaveriseapp.presentation.search.SearchScreen
import com.wordwaverise.wordwaveriseapp.presentation.search.SearchViewModel
import com.wordwaverise.wordwaveriseapp.presentation.study.StudyScreen
import com.wordwaverise.wordwaveriseapp.data.local.SettingsDataStore
import com.wordwaverise.wordwaveriseapp.ui.theme.ThemeMode
import com.wordwaverise.wordwaveriseapp.ui.theme.WordWaveriseAppTheme

/**
 * Ссылка с сайта, которую приложение открывает вместо браузера.
 *
 * Разбирается один раз здесь, а не в каждом экране: `/f/` и `/g/` — единственные пути, которые
 * заявлены в манифесте, и держать их список в двух местах значит однажды заявить путь, который
 * приложение не умеет открыть.
 */
private sealed interface PendingLink {
    /** Общая папка: `https://wordwaverise.com/f/{token}`. */
    data class SharedFolder(val token: String) : PendingLink

    /** Приглашение в группу: `https://wordwaverise.com/g/{token}`. */
    data class GroupInvite(val token: String) : PendingLink
}

private fun parseLink(intent: Intent?): PendingLink? {
    if (intent?.action != Intent.ACTION_VIEW) return null
    val segments = intent.data?.pathSegments ?: return null
    if (segments.size < 2) return null
    val token = segments[1].takeIf { it.isNotBlank() } ?: return null
    return when (segments[0]) {
        "f" -> PendingLink.SharedFolder(token)
        "g" -> PendingLink.GroupInvite(token)
        else -> null
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: SettingsDataStore

    /**
     * Ссылка ждёт, пока её будет чем открыть.
     *
     * Приглашение в группу приходит человеку, у которого приложения ещё не было, — то есть чаще
     * всего его нажимают до входа. Ссылка, применённая на экране логина, просто пропала бы, и
     * второй раз её никто не пришлёт.
     */
    private var pendingLink by mutableStateOf<PendingLink?>(null)

    /**
     * ⚠️ Нужен вместе с `launchMode="singleTop"`: при работающем приложении система не создаёт
     * activity заново, а приносит интент сюда. Без этого нажатая ссылка молча ничего не делала
     * бы — ровно в том случае, когда приложение уже открыто, то есть в самом частом.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseLink(intent)?.let { pendingLink = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingLink = parseLink(intent)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsDataStore.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            WordWaveriseAppTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.state

                val currentRoute = navController.currentBackStackEntryAsState().value
                    ?.destination?.route?.substringBefore('?')
                // Экраны во весь экран: у статьи слова и у страницы книги нижней панели нет.
                // Набор, а не одно сравнение, — второй такой экран уже появился.
                val fullScreenRoutes = setOf(Screen.WordDetail.route, Screen.Reader.route)
                val showBottomBar = authState.isLoggedIn && currentRoute !in fullScreenRoutes

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
                            onLoginChange = authViewModel::onLoginChange,
                            onPasswordChange = authViewModel::onPasswordChange,
                            onLogin = authViewModel::login,
                            onRegister = authViewModel::register,
                            onLoginWithGoogle = authViewModel::loginWithGoogle,
                            onVerificationCodeChange = authViewModel::onVerificationCodeChange,
                            onVerifyEmail = authViewModel::verifyEmail,
                            onResendCode = authViewModel::resendVerificationCode,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        // Ссылка применяется только после входа: экраны, которые её принимают,
                        // до этого момента не существуют. Сбрасывается сразу после перехода,
                        // иначе поворот экрана открывал бы приглашение заново.
                        LaunchedEffect(pendingLink) {
                            when (val link = pendingLink) {
                                is PendingLink.SharedFolder ->
                                    navController.navigate(Screen.Study.createImportRoute(link.token))
                                is PendingLink.GroupInvite ->
                                    navController.navigate(Screen.Groups.createInviteRoute(link.token))
                                null -> Unit
                            }
                            if (pendingLink != null) pendingLink = null
                        }

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
                                    onClear = viewModel::clearSearch,
                                    isPlayingAudio = viewModel.state.value.isPlayingAudio,
                                    playingAudioUrl = viewModel.state.value.playingAudioUrl,
                                    onPlayAudio = viewModel::playAudio,
                                    onStopAudio = viewModel::stopAudio,
                                    onWordClick = { word ->
                                        navController.navigate(Screen.WordDetail.createRoute(word))
                                    },
                                    onSelectSuggestion = viewModel::selectSuggestion,
                                    onSearchOriginal = viewModel::searchOriginalQuery,
                                    onTokenClick = viewModel::analyzeToken,
                                    pinnedSenseIds = viewModel.pinnedSenseIds.value,
                                    onToggleSense = viewModel::toggleSense,
                                    onToggleSaveFolder = viewModel::toggleSaveFolder,
                                    onClearSaveFolders = viewModel::clearSaveFolders,
                                    onCreateSaveFolder = viewModel::createFolderForSave,
                                    onConfirmSave = viewModel::confirmSaveSense,
                                    onCancelSave = viewModel::cancelSaveSense
                                )
                            }

                            /**
                             * «Учёба» — слова и задания под одной вкладкой.
                             *
                             * ⚠️ Один маршрут на обе половины: подсветка вкладки сравнивает
                             * маршрут целиком, и на втором она бы гасла. Оба необязательных
                             * аргумента ссылок (`/f/{token}` и переход по заданию) приезжают
                             * сюда же и называют сегмент, который надо открыть.
                             */
                            composable(
                                route = Screen.Study.ROUTE_FULL,
                                arguments = listOf(
                                    navArgument("segment") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("import") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("assignment") {
                                        type = NavType.IntType
                                        defaultValue = Screen.Study.NO_ASSIGNMENT
                                    }
                                )
                            ) { entry ->
                                StudyScreen(
                                    startSegment = entry.arguments?.getString("segment"),
                                    importToken = entry.arguments?.getString("import"),
                                    startAssignmentId = entry.arguments
                                        ?.getInt("assignment")
                                        ?.takeIf { it != Screen.Study.NO_ASSIGNMENT },
                                    onWordClick = { word ->
                                        navController.navigate(
                                            Screen.WordDetail.createRoute(word, exact = true)
                                        )
                                    }
                                )
                            }

                            composable(Screen.Books.route) {
                                BooksScreen(
                                    onOpenBook = { bookId ->
                                        navController.navigate(Screen.Reader.createRoute(bookId))
                                    }
                                )
                            }

                            composable(
                                route = Screen.Reader.route,
                                arguments = listOf(
                                    navArgument("bookId") { type = NavType.IntType }
                                )
                            ) { entry ->
                                ReaderScreen(
                                    bookId = entry.arguments?.getInt("bookId") ?: 0,
                                    onBack = { navController.popBackStack() },
                                    onOpenArticle = { word ->
                                        navController.navigate(Screen.WordDetail.createRoute(word))
                                    }
                                )
                            }

                            composable(Screen.Profile.route) {
                                val profileViewModel: ProfileViewModel = hiltViewModel()
                                val profileState by profileViewModel.state.collectAsState()
                                val currentThemeMode by profileViewModel.themeMode.collectAsState()
                                ProfileScreen(
                                    userEmail = authState.userEmail ?: "",
                                    userLogin = authState.userLogin,
                                    state = profileState,
                                    themeMode = currentThemeMode,
                                    onThemeModeChange = profileViewModel::setThemeMode,
                                    onLogout = { authViewModel.logout() },
                                    deletionScheduledFor = authState.deletionScheduledFor,
                                    deletionLoading = authState.deletionActionLoading,
                                    deletionError = authState.deletionError,
                                    hasPassword = authState.hasPassword,
                                    onRequestDeletion = { password, googleIdToken ->
                                        authViewModel.requestAccountDeletion(password, googleIdToken)
                                    },
                                    onCancelDeletion = { authViewModel.cancelAccountDeletion() },
                                    onClearDeletionError = { authViewModel.clearDeletionError() },
                                    onOpenGroups = { navController.navigate(Screen.Groups.route) }
                                )
                            }

                            composable(
                                route = Screen.Groups.ROUTE_WITH_INVITE,
                                arguments = listOf(
                                    navArgument("invite") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { entry ->
                                val inviteToken = entry.arguments?.getString("invite")
                                GroupsScreen(
                                    joinInviteToken = inviteToken,
                                    onBack = { navController.popBackStack() },
                                    onPractise = { assignmentId ->
                                        // Задание выполняется там же, где обычная практика:
                                        // сессия та же, отличается только откуда взялись
                                        // папка и типы вопросов.
                                        navController.navigate(
                                            Screen.Study.createAssignmentRoute(assignmentId)
                                        )
                                    }
                                )
                            }

                            composable(
                                route = Screen.WordDetail.route,
                                arguments = listOf(
                                    navArgument("word") { type = NavType.StringType },
                                    navArgument("exact") {
                                        type = NavType.BoolType
                                        defaultValue = false
                                    }
                                )
                            ) {
                                val viewModel: WordDetailViewModel = hiltViewModel()
                                val state by viewModel.state.collectAsState()
                                WordDetailScreen(
                                    wordDetail = state.wordDetail,
                                    entry = state.entry,
                                    annotationPending = state.annotationPending,
                                    annotationDegraded = state.annotationDegraded,
                                    isLoading = state.isLoading,
                                    isLoadingFull = state.isLoadingFull,
                                    error = state.error,
                                    pinnedSenseIds = state.pinnedSenseIds,
                                    onToggleSense = viewModel::toggleSense,
                                    pendingSenseId = state.pendingSenseId,
                                    pendingSenseSummary = state.pendingSenseSummary,
                                    ownFolders = state.ownFolders,
                                    chosenFolders = state.chosenFolders,
                                    isSavingSense = state.isSavingSense,
                                    onToggleSaveFolder = viewModel::toggleSaveFolder,
                                    onClearSaveFolders = viewModel::clearSaveFolders,
                                    onCreateSaveFolder = viewModel::createFolderForSave,
                                    onConfirmSave = viewModel::confirmSaveSense,
                                    onCancelSave = viewModel::cancelSaveSense,
                                    isPlayingAudio = state.isPlayingAudio,
                                    playingAudioUrl = state.playingAudioUrl,
                                    onPlayAudio = viewModel::playAudio,
                                    onStopAudio = viewModel::stopAudio,
                                    onBack = { navController.popBackStack() },
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
