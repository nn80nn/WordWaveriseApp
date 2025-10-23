package n.startapp.wordwaveriseapp.presentation.main

data class MainState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)
