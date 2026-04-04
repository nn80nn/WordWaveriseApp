package n.startapp.wordwaveriseapp.presentation.saved

import n.startapp.wordwaveriseapp.data.local.entity.CategoryEntity
import n.startapp.wordwaveriseapp.data.local.entity.SavedWordEntity

data class SavedWordsState(
    val words: List<SavedWordEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isOffline: Boolean = false,
    val showCategorySheet: Boolean = false,
    val wordToMove: String? = null,
    val newCategoryName: String = ""
) {
    val filteredWords: List<SavedWordEntity>
        get() = if (selectedCategoryId == null) words
                else words.filter { it.categoryId == selectedCategoryId }
}
