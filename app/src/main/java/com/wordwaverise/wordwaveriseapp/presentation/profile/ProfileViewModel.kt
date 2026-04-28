package com.wordwaverise.wordwaveriseapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.wordwaverise.wordwaveriseapp.data.local.dao.FlashcardDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.SavedWordDao
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedWordDao: SavedWordDao,
    flashcardDao: FlashcardDao
) : ViewModel() {

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
