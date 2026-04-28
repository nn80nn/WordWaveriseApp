package com.wordwaverise.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PronunciationEntry(
    val region: String? = null,       // "uk" | "us" | null
    val ipa: String? = null,
    val audioMp3Url: String? = null
)

@Serializable
data class Definition(
    val partOfSpeech: String,
    val definition: String,
    val example: String? = null,
    val source: String? = null
)

/** One definition within a WordEntry. */
@Serializable
data class EntryMeaning(
    val definition: String = "",
    val example: String? = null,
    val source: String? = null
)

/** A single entry (homograph) grouped by part of speech. */
@Serializable
data class WordEntry(
    val id: String = "",
    val partOfSpeech: String? = null,
    val phonetic: String? = null,
    val audioUrl: String? = null,
    val pronunciations: List<PronunciationEntry> = emptyList(),
    val meanings: List<EntryMeaning> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
    val translation: String? = null   // Russian translation for this POS (e.g. "свинец" for noun, "вести" for verb)
)

@Serializable
data class WordDetailResponse(
    val word: String,
    val phonetic: String? = null,
    val audioUrl: String? = null,
    val pronunciations: List<PronunciationEntry> = emptyList(),
    val translation: String? = null,
    val definitions: List<Definition> = emptyList(),
    val entries: List<WordEntry> = emptyList(),     // grouped by POS (homographs)
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val examples: List<String> = emptyList()
)

/** Envelope wrapper: {"status":"ok","data":{...WordDetailResponse...}} */
@Serializable
data class WordDetailApiResponse(
    val status: String,
    val data: WordDetailResponse? = null,
    val message: String? = null
)
