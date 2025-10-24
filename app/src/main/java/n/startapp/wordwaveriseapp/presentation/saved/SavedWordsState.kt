package n.startapp.wordwaveriseapp.presentation.saved

import n.startapp.wordwaveriseapp.data.local.entity.SavedWordEntity

data class SavedWordsState(
    val words: List<SavedWordEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)
