package com.wordwaverise.wordwaveriseapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.local.SettingsDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.FlashcardDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.SavedWordDao
import com.wordwaverise.wordwaveriseapp.ui.theme.ThemeMode
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedWordDao: SavedWordDao,
    flashcardDao: FlashcardDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val themeMode = settingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    val state = combine(
        savedWordDao.getCount(),
        flashcardDao.getTotalCount(),
        flashcardDao.getDueCount()
    ) { savedCount, totalFlashcards, dueFlashcards ->
        ProfileState(
            savedWordsCount = savedCount,
            totalFlashcards = totalFlashcards,
            dueFlashcards = dueFlashcards
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileState()
    )
}
