package n.startapp.wordwaveriseapp.presentation.search

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import n.startapp.wordwaveriseapp.data.repository.SearchRepository
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SearchViewModel"
    }

    private val _state = mutableStateOf(SearchState())
    val state: State<SearchState> = _state

    fun onSearchQueryChange(query: String) {
        Log.d(TAG, "Search query changed: $query")
        _state.value = _state.value.copy(
            searchQuery = query,
            error = null
        )
    }

    fun searchWord() {
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) {
            Log.w(TAG, "Search attempted with empty query")
            _state.value = _state.value.copy(
                error = "Пожалуйста, введите слово для поиска"
            )
            return
        }

        Log.d(TAG, "Starting search for: $query")
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                wordData = null,
                hasSearched = false
            )
            Log.d(TAG, "State set to loading")

            when (val result = searchRepository.searchWord(query)) {
                is Resource.Success -> {
                    Log.d(TAG, "Search success! Word data: ${result.data}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        wordData = result.data,
                        error = null,
                        hasSearched = true
                    )
                    Log.d(TAG, "State updated with word data. Current state: ${_state.value}")
                }
                is Resource.Error -> {
                    Log.e(TAG, "Search error: ${result.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message,
                        wordData = null,
                        hasSearched = true
                    )
                    Log.d(TAG, "State updated with error. Current state: ${_state.value}")
                }
                is Resource.Loading -> {
                    Log.d(TAG, "Resource loading state")
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun clearSearch() {
        Log.d(TAG, "Clearing search")
        _state.value = SearchState()
    }
}
