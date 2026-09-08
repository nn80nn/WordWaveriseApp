package com.wordwaverise.wordwaveriseapp.data.remote.dto.reader

import kotlinx.serialization.Serializable
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.TokenDto

/**
 * Библиотека читалки — зеркало `models/reader/ReaderDtos.kt` на сервере.
 *
 * ⚠️ Сервер сериализует без `encodeDefaults`, поэтому поле, равное умолчанию, из JSON просто
 * пропадает: `author`, `position`, `sentences`, `tokens`, `nextOrdinal`. Умолчание здесь — не
 * удобство, а условие того, что первая же книга без автора не уронит разбор ответа.
 *
 * [TokenDto] переиспользуется, а не дублируется: в книге и в разборе предложения это буквально
 * один и тот же тип, и два его описания разошлись бы на первой правке токенизатора.
 */

@Serializable
data class SentenceDto(
    val index: Int = 0,
    val text: String = "",
    /** Смещения **в блоке** — ими предложение подсвечивается на месте. */
    val start: Int = 0,
    val end: Int = 0,
    /** ⚠️ А вот смещения токенов считаются от **этого предложения**, не от блока. */
    val tokens: List<TokenDto> = emptyList()
)

@Serializable
data class BlockDto(
    /** Сквозной по всей книге — адрес, который хранит позиция чтения. */
    val ordinal: Int = 0,
    val chapterIndex: Int = 0,
    /** `HEADING` | `PARAGRAPH` | `QUOTE` | `LIST_ITEM`. */
    val kind: String = "PARAGRAPH",
    val text: String = "",
    val sentences: List<SentenceDto> = emptyList()
)

@Serializable
data class ChapterDto(
    val index: Int = 0,
    val title: String? = null,
    val firstOrdinal: Int = 0,
    val blockCount: Int = 0
)

@Serializable
data class ReadingPositionDto(
    val ordinal: Int = 0,
    val chapterIndex: Int = 0,
    /** 0..1, считается сервером по числу блоков. Клиент его не присылает. */
    val progress: Double = 0.0,
    val updatedAt: String = ""
)

@Serializable
data class BookDto(
    val id: Int = 0,
    val title: String = "",
    val author: String? = null,
    val language: String? = null,
    /** `EPUB` | `FB2` | `TXT` | `HTML` | `PASTE`. */
    val format: String = "",
    val chapterCount: Int = 0,
    val blockCount: Int = 0,
    val wordCount: Int = 0,
    val createdAt: String = "",
    /** Пусто, пока книгу ни разу не открывали. */
    val position: ReadingPositionDto? = null
)

@Serializable
data class BookDetailDto(
    val book: BookDto = BookDto(),
    val chapters: List<ChapterDto> = emptyList()
)

@Serializable
data class BlockPageDto(
    val bookId: Int = 0,
    val from: Int = 0,
    val blocks: List<BlockDto> = emptyList(),
    /** Что просить дальше; `null` — конец книги. */
    val nextOrdinal: Int? = null
)

@Serializable
data class BookImportDto(
    val book: BookDto = BookDto(),
    /**
     * Тот же текст уже лежал на полке.
     *
     * Не ошибка: сервер узнал его по хэшу текста (вместе с названием) и вернул ту книгу, что
     * есть, вместе с местом, где её бросили. Правильный ход — открыть её там же.
     */
    val alreadyExisted: Boolean = false
)

@Serializable
data class ImportBookTextRequest(
    val text: String,
    val title: String? = null,
    val author: String? = null
)

/**
 * Место, которое читатель отметил сам.
 *
 * [preview] — начало отмеченного абзаца: список закладок без него это список чисел, по которому
 * нельзя узнать ни одно из отмеченных мест.
 */
@Serializable
data class BookmarkDto(
    val ordinal: Int = 0,
    val chapterIndex: Int = 0,
    val preview: String = "",
    val createdAt: String = ""
)

@Serializable
data class SetBookmarkRequest(val ordinal: Int)

@Serializable
data class BookmarksResponse(
    val status: String = "",
    val data: List<BookmarkDto>? = null,
    val message: String? = null
)

@Serializable
data class BookmarkResponse(
    val status: String = "",
    val data: BookmarkDto? = null,
    val message: String? = null
)

@Serializable
data class SetPositionRequest(val ordinal: Int)

// ── Конверты ──────────────────────────────────────────────────────────

@Serializable
data class BooksResponse(
    val status: String = "",
    val data: List<BookDto>? = null,
    val message: String? = null
)

@Serializable
data class BookDetailResponse(
    val status: String = "",
    val data: BookDetailDto? = null,
    val message: String? = null
)

@Serializable
data class BlockPageResponse(
    val status: String = "",
    val data: BlockPageDto? = null,
    val message: String? = null
)

@Serializable
data class BookImportResponse(
    val status: String = "",
    val data: BookImportDto? = null,
    val message: String? = null
)

@Serializable
data class ReadingPositionResponse(
    val status: String = "",
    val data: ReadingPositionDto? = null,
    val message: String? = null
)
