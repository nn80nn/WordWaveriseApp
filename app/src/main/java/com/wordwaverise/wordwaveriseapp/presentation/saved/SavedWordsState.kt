package com.wordwaverise.wordwaveriseapp.presentation.saved

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity

data class SavedWordsState(
    val words: List<SavedWordEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isOffline: Boolean = false,
    val showCategorySheet: Boolean = false,
    val wordToMove: String? = null,
    val newCategoryName: String = "",
    /** Ссылка, которую надо отдать системному листу «Поделиться»; одноразовая. */
    val pendingShareUrl: String? = null,
    /** Ссылка на чужую папку, которую человек вставил. */
    val importLink: String = "",
    val importing: Boolean = false,
    /** Итог добавления словами — включая то, что осталось лежать на своих местах. */
    val importMessage: String? = null
) {
    val filteredWords: List<SavedWordEntity>
        get() = if (selectedCategoryId == null) words
                else words.filter { it.categoryId == selectedCategoryId }
}
