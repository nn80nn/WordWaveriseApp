package com.wordwaverise.wordwaveriseapp.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.GroupDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.StudentAssignmentDto
import com.wordwaverise.wordwaveriseapp.data.repository.CategoryRepository
import com.wordwaverise.wordwaveriseapp.data.repository.GroupRepository
import com.wordwaverise.wordwaveriseapp.data.repository.SavedWordsRepository
import com.wordwaverise.wordwaveriseapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsState(
    val owned: List<GroupDto> = emptyList(),
    val joined: List<GroupDto> = emptyList(),
    val assignments: List<StudentAssignmentDto> = emptyList(),
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val error: String? = null,
    val message: String? = null
) {
    val all: List<GroupDto> get() = owned + joined
    val hasAny: Boolean get() = all.isNotEmpty()
    val openAssignments: Int get() = assignments.count { !it.completed }
}

/**
 * Классы ученика на телефоне.
 *
 * Администрирования здесь нет вовсе — оно целиком в вебе. На телефоне можно вступить, выйти,
 * увидеть, что задали, и это выполнить; всё остальное требует экрана, которого у ученика нет.
 */
@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val categoryRepository: CategoryRepository,
    private val savedWordsRepository: SavedWordsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GroupsState())
    val state: StateFlow<GroupsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val groups = groupRepository.myGroups()) {
                is Resource.Success -> _state.update {
                    it.copy(
                        owned = groups.data?.owned.orEmpty(),
                        joined = groups.data?.joined.orEmpty(),
                        isLoading = false
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, error = groups.message)
                }
                is Resource.Loading -> Unit
            }

            when (val assignments = groupRepository.assignments()) {
                is Resource.Success ->
                    _state.update { it.copy(assignments = assignments.data.orEmpty()) }
                else -> Unit
            }
        }
    }

    fun joinByCode(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, error = null) }
            when (val result = groupRepository.joinByCode(code)) {
                is Resource.Success -> {
                    _state.update { it.copy(isJoining = false, message = "Вы в группе") }
                    // Папки преподавателя должны появиться сразу, а не после следующего входа.
                    syncBorrowedContent()
                    refresh()
                }
                is Resource.Error -> _state.update {
                    it.copy(isJoining = false, error = result.message ?: "Код не подошёл")
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun leave(groupId: Int) {
        viewModelScope.launch {
            when (val result = groupRepository.leave(groupId)) {
                is Resource.Success -> {
                    _state.update { it.copy(message = "Вы вышли из группы") }
                    // Папка уходит вместе с членством; карточки остаются, просто без папки.
                    syncBorrowedContent()
                    refresh()
                }
                is Resource.Error -> _state.update { it.copy(error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    private suspend fun syncBorrowedContent() {
        categoryRepository.syncCategories()
        savedWordsRepository.syncWords()
    }
}
