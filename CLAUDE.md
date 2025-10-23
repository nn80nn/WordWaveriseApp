# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WordWaveriseApp is an Android application built with Kotlin and Jetpack Compose. The app follows modern Android development practices using Material Design 3, with support for dynamic theming on Android 12+.

**Key Technologies:**
- Language: Kotlin 2.0.21
- UI Framework: Jetpack Compose (Material3)
- Build System: Gradle with Kotlin DSL
- Dependency Injection: Hilt 2.51
- Networking: Retrofit 2.11.0 + OkHttp 4.12.0
- Serialization: Kotlinx Serialization 1.6.3
- Min SDK: 26 (Android 8.0)
- Target SDK: 36
- Java Version: 11

## Build & Development Commands

### Building the App
```bash
# Build debug APK
.\gradlew assembleDebug

# Build release APK
.\gradlew assembleRelease

# Install debug build on connected device
.\gradlew installDebug

# Clean build
.\gradlew clean
```

### Running Tests
```bash
# Run all unit tests
.\gradlew test

# Run unit tests for debug variant
.\gradlew testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
.\gradlew connectedAndroidTest

# Run specific test class
.\gradlew test --tests n.startapp.wordwaveriseapp.ExampleUnitTest

# Run specific test method
.\gradlew test --tests n.startapp.wordwaveriseapp.ExampleUnitTest.addition_isCorrect
```

### Code Quality
```bash
# Lint check
.\gradlew lint

# Generate lint report
.\gradlew lintDebug
```

## Architecture & Code Organization

### Clean Architecture with MVVM
The app follows Clean Architecture principles with MVVM pattern:

```
app/src/main/java/n/startapp/wordwaveriseapp/
├── data/
│   ├── remote/
│   │   ├── dto/           # Data Transfer Objects
│   │   └── ApiService.kt  # Retrofit API interface
│   └── repository/        # Repository implementations
├── di/
│   └── NetworkModule.kt   # Hilt dependency injection modules
├── presentation/
│   └── main/              # Main screen (feature-based organization)
│       ├── MainScreen.kt  # Composable UI
│       ├── MainState.kt   # UI State
│       └── MainViewModel.kt # ViewModel
├── ui/theme/              # App theming
├── util/                  # Utilities (Resource wrapper, error handling)
├── MainActivity.kt        # Main activity (annotated with @AndroidEntryPoint)
└── WordWaveriseApplication.kt # Application class (annotated with @HiltAndroidApp)
```

### Dependency Injection (Hilt)
- **Application class**: `WordWaveriseApplication` is annotated with `@HiltAndroidApp`
- **Activities**: Annotated with `@AndroidEntryPoint` to enable injection
- **ViewModels**: Annotated with `@HiltViewModel` and use constructor injection
- **Modules**: Located in `di/` package
  - `NetworkModule`: Provides Retrofit, OkHttp, ApiService

### Network Layer
- **Base URL**: Configured in `BuildConfig.BASE_URL` (default: `http://10.0.2.2:3000/` for Android emulator)
- **Retrofit**: JSON serialization using Kotlinx Serialization
- **OkHttp**: Includes logging interceptor (only in debug builds)
- **Error Handling**: Centralized in `NetworkError.getErrorMessage()` with localized error messages
- **Resource Wrapper**: `Resource<T>` sealed class for Success/Error/Loading states

### Presentation Layer
- **State Management**: Each screen has a dedicated `State` data class
- **ViewModels**: Use `viewModelScope` for coroutines
- **Composables**: Pure functions receiving state and event callbacks
- **MainActivity**: Single activity pattern with Compose

### Theme System
Located in `ui.theme/` package:
- `Theme.kt`: Main theme composable with dark/light mode support and dynamic color (Android 12+)
- `Color.kt`: Color definitions for Purple/Pink palette
- `Type.kt`: Typography definitions

The app uses Material3's theming system with:
- Dynamic color scheme support (Android 12+)
- System theme detection via `isSystemInDarkTheme()`
- Fallback color schemes for older Android versions

### Testing Structure
- **Unit tests**: `app/src/test/java/` - JUnit tests that run on the JVM
- **Instrumented tests**: `app/src/androidTest/java/` - Android-specific tests requiring device/emulator

## Dependency Management

Dependencies are managed via Gradle version catalogs in `gradle/libs.versions.toml`. When adding new dependencies:
1. Define the version in `[versions]`
2. Add the library in `[libraries]` with group, name, and version reference
3. Reference in `app/build.gradle.kts` using `libs.` prefix

## Important Configuration Details

### Build Configuration
- Namespace: `n.startapp.wordwaveriseapp`
- Application ID: `n.startapp.wordwaveriseapp`
- Compile SDK: 36
- ProGuard is disabled for release builds (set `isMinifyEnabled = false`)

### Compose Configuration
- Compose is enabled via `buildFeatures { compose = true }`
- Uses Compose BOM (Bill of Materials) for version alignment
- Kotlin Compose Compiler plugin is applied

## Development Notes

### API Integration
- The app connects to a backend API at the URL specified in `BuildConfig.BASE_URL`
- Default URL is `http://10.0.2.2:3000/` which points to localhost on the Android emulator
- For testing on physical devices, update the base URL in `app/build.gradle.kts` to your machine's IP address
- Currently implements `/api/health` endpoint for connection testing

### Network Configuration
- Internet permission is declared in `AndroidManifest.xml`
- Network requests are made using Kotlin coroutines with `suspend` functions
- All network calls should be wrapped in try-catch and return `Resource<T>` type

### Adding New API Endpoints
1. Define the response DTO in `data/remote/dto/`
2. Add the endpoint function in `ApiService.kt`
3. Create/update the repository in `data/repository/`
4. Use the repository in ViewModels with proper error handling

### Common Patterns
- **ViewModels**: Always use `viewModelScope.launch` for coroutines
- **State Updates**: Immutably update state using `copy()`
- **Error Messages**: Use `NetworkError.getErrorMessage()` for consistent error handling
- **Loading States**: Set `isLoading = true` before network calls, `false` after completion
