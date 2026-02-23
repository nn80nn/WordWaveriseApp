package n.startapp.wordwaveriseapp.presentation.detail

import n.startapp.wordwaveriseapp.data.remote.dto.WordDetailResponse

data class WordDetailState(
    val word: String = "",
    val isLoading: Boolean = false,
    val wordDetail: WordDetailResponse? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isSavedLoading: Boolean = true,
    val isPlayingAudio: Boolean = false,
    val playingAudioUrl: String? = null,
    val audioError: String? = null
)
