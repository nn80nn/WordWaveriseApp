package com.wordwaverise.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Одна подсказка под строкой поиска.
 *
 * ⚠️ Голое написание подсказкой в словаре не работает: шесть английских слов не говорят,
 * какое из них имелось в виду, и человек всё равно открывает их по очереди — то есть делает
 * ту работу, ради экономии которой подсказки и нужны. Перевод с частью речи есть у слова,
 * чья статья уже написана, и стоят они ноль.
 */
@Serializable
data class SuggestItemDto(
    val word: String = "",
    val translation: String? = null,
    val partOfSpeech: String? = null,
    /** `corpus` | `autocomplete` | `spelling` | `translation` — см. бэкенд `SuggestItem`. */
    val kind: String = "autocomplete",
    val inCorpus: Boolean = false
)

@Serializable
data class SuggestDto(
    val query: String = "",
    val lang: String = "",
    /** Те же подсказки строками — что отвечал сервер до появления [items]. */
    val suggestions: List<String> = emptyList(),
    val items: List<SuggestItemDto> = emptyList()
)

@Serializable
data class SuggestApiResponse(
    val status: String = "",
    val data: SuggestDto? = null
)
