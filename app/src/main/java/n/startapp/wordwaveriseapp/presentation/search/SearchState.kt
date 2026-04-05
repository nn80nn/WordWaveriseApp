package n.startapp.wordwaveriseapp.presentation.search

import n.startapp.wordwaveriseapp.data.remote.dto.WordDto

data class SearchState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val wordData: WordDto? = null,
    val error: String? = null,
    val hasSearched: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val playingAudioUrl: String? = null,
    val suggestions: List<String> = emptyList(),
    val isFetchingSuggestions: Boolean = false,
    val isRussianSearch: Boolean = false,
    val russianQuery: String = ""
)
