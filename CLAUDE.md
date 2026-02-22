# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WordWaveriseApp is an Android vocabulary/dictionary app built with Kotlin and Jetpack Compose. Users search words, save them, and study via flashcards. The UI strings are in Russian.

**Key Technologies:**
- Language: Kotlin 2.0.21, Java 11
- UI: Jetpack Compose + Material Design 3 (dynamic theming on Android 12+)
- Build: Gradle with Kotlin DSL, version catalog (`gradle/libs.versions.toml`)
- DI: Hilt 2.51
- Networking: Retrofit 2.11.0 + OkHttp 4.12.0 + Kotlinx Serialization 1.6.3
- Local storage: Room (v2 database), DataStore Preferences
- Min SDK: 26, Target SDK: 36

## Build & Development Commands

```bash
# Build debug APK
.\gradlew assembleDebug

# Build release APK
.\gradlew assembleRelease

# Install on connected device
.\gradlew installDebug

# Run unit tests
.\gradlew testDebugUnitTest

# Run specific test class
.\gradlew test --tests n.startapp.wordwaveriseapp.ExampleUnitTest

# Run instrumented tests (requires device/emulator)
.\gradlew connectedAndroidTest

# Lint
.\gradlew lintDebug
```

## Architecture

### Layer Structure (Clean Architecture + MVVM)

```
data/
  remote/
    dto/           # DTOs: auth/, saved/, flashcard/, WordDetailResponse, etc.
    ApiService.kt  # Retrofit interface — all endpoints in one place
  local/
    entity/        # Room entities: SavedWordEntity, FlashcardEntity
    dao/           # SavedWordDao, FlashcardDao
    AppDatabase.kt # Room DB (version 2, fallbackToDestructiveMigration)
    TokenDataStore.kt  # JWT + user email via DataStore Preferences
  repository/      # AuthRepository, SearchRepository, SavedWordsRepository,
                   # FlashcardRepository, HealthRepository
di/
  NetworkModule.kt  # Retrofit, OkHttp, ApiService (Singleton)
  DatabaseModule.kt # AppDatabase, DAOs, TokenDataStore (Singleton)
presentation/
  auth/            # AuthScreen, AuthViewModel, AuthState
  search/          # SearchScreen, SearchViewModel, SearchState
  saved/           # SavedScreen, SavedWordsViewModel, SavedWordsState
  tasks/           # TasksScreen, TasksViewModel, TasksState
  profile/         # ProfileScreen (uses AuthViewModel for logout)
  detail/          # WordDetailScreen, WordDetailViewModel, WordDetailState (NOT yet in nav)
  navigation/      # Screen.kt (sealed class), BottomNavigationBar.kt
  main/            # MainScreen, MainViewModel, MainState
ui/theme/          # Theme.kt, Color.kt, Type.kt
util/              # Resource<T> sealed class (Success/Error/Loading), NetworkError
```

### Navigation & Auth Flow

`MainActivity` hosts all navigation. `AuthViewModel` reads the persisted JWT token from `TokenDataStore` on startup — if `authState.isLoggedIn` is false, the `AuthScreen` is shown; otherwise a `NavHost` with a `BottomNavigationBar` is shown.

The four bottom nav tabs are defined in `Screen.kt` as `Search`, `Saved`, `Tasks`, `Profile`.

**Note:** `WordDetailScreen` with its `WordDetailViewModel` has been built (see `WORD_DETAIL_INTEGRATION.md`) but is not yet wired into the `NavHost` in `MainActivity`. Integration steps are documented there.

### Dependency Injection

- `WordWaveriseApplication` → `@HiltAndroidApp`
- `MainActivity` → `@AndroidEntryPoint`
- ViewModels → `@HiltViewModel` with constructor injection
- Repositories → `@Singleton` with `@Inject constructor`

### Network Layer

- **Base URL**: `https://backend.wordwaverise.com/` (set in `app/build.gradle.kts` as `BuildConfig.BASE_URL`)
- Auth tokens are passed **per-request** as `@Header("Authorization")` — there is no OkHttp auth interceptor
- `Json { ignoreUnknownKeys = true; isLenient = true }` in `NetworkModule`
- All API calls return `Resource<T>` using try-catch in the repository; use `NetworkError.getErrorMessage(e)` for errors

### Local Database

Room database `wordwaverise_database` (v2) with two tables:
- `SavedWordEntity` — local cache of saved words
- `FlashcardEntity` — flashcard study data

`DatabaseModule` uses `fallbackToDestructiveMigration()` — **replace with proper migrations before production release**.

### Dependency Management

Add new dependencies via `gradle/libs.versions.toml`:
1. Add version in `[versions]`
2. Add library in `[libraries]` with group, name, and version reference
3. Reference in `app/build.gradle.kts` as `libs.<name>`

### Adding New Features

**New API endpoint:**
1. Add DTO in `data/remote/dto/`
2. Add function to `ApiService.kt`
3. Add/update repository in `data/repository/` — wrap in `Resource<T>` with try-catch
4. Inject repository into ViewModel

**New screen:**
1. Create `presentation/<feature>/` with `Screen.kt`, `ViewModel.kt`, `State.kt`
2. Add route to `Screen.kt` sealed class if it's a nav destination
3. Add `composable()` block in `MainActivity` NavHost
