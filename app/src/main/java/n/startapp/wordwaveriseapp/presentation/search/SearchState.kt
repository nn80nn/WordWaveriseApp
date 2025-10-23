package n.startapp.wordwaveriseapp.presentation.search

import n.startapp.wordwaveriseapp.data.remote.dto.WordDto

data class SearchState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val wordData: WordDto? = null,
    val error: String? = null,
    val hasSearched: Boolean = false
)
