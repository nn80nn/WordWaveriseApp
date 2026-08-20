package com.wordwaverise.wordwaveriseapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.local.SettingsDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.FlashcardDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.SavedWordDao
import com.wordwaverise.wordwaveriseapp.data.repository.GroupRepository
import com.wordwaverise.wordwaveriseapp.util.Resource
import com.wordwaverise.wordwaveriseapp.ui.theme.ThemeMode
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedWordDao: SavedWordDao,
    flashcardDao: FlashcardDao,
    private val settingsDataStore: SettingsDataStore,
    private val groupRepository: GroupRepository
) : ViewModel() {

    /**
     * Классы приходят по сети, а не из Room: своей таблицы у них нет.
     *
     * ⚠️ Это первая сетевая зависимость Профиля, поэтому она отдельным потоком, а не внутри
     * `combine` ниже: сбой запроса не должен обнулять счётчики, которые прекрасно живут офлайн.
     */
    private val groupSummary = MutableStateFlow(0 to 0)

    val themeMode = settingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    init {
        refreshGroups()
    }

    fun refreshGroups() {
        viewModelScope.launch {
            val groups = (groupRepository.myGroups() as? Resource.Success)?.data
            val assignments = (groupRepository.assignments() as? Resource.Success)?.data
            val count = (groups?.owned?.size ?: 0) + (groups?.joined?.size ?: 0)
            val open = assignments?.count { !it.completed } ?: 0
            groupSummary.value = count to open
        }
    }

    val state = combine(
        savedWordDao.getCount(),
        flashcardDao.getTotalCount(),
        flashcardDao.getDueCount(),
        groupSummary
    ) { savedCount, totalFlashcards, dueFlashcards, groups ->
        ProfileState(
            savedWordsCount = savedCount,
            totalFlashcards = totalFlashcards,
            dueFlashcards = dueFlashcards,
            groupCount = groups.first,
            openAssignments = groups.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileState()
    )
}
