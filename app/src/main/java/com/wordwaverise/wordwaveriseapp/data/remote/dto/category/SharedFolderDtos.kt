package com.wordwaverise.wordwaveriseapp.data.remote.dto.category

import kotlinx.serialization.Serializable

/** Ссылка на папку. Повторный запрос возвращает ту же — отправленная не должна протухать. */
@Serializable
data class ShareLinkDto(
    val token: String = "",
    val url: String = ""
)

@Serializable
data class ShareLinkResponse(
    val status: String,
    val data: ShareLinkDto? = null,
    val message: String? = null
)

@Serializable
data class SharedWordPreviewDto(
    val word: String = "",
    val translation: String? = null
)

/** Папка глазами получателя. Автора здесь нет намеренно: ссылка ходит дальше отправителя. */
@Serializable
data class SharedFolderPreviewDto(
    val name: String = "",
    val wordCount: Int = 0,
    val sample: List<SharedWordPreviewDto> = emptyList()
)

@Serializable
data class SharedFolderPreviewResponse(
    val status: String,
    val data: SharedFolderPreviewDto? = null,
    val message: String? = null
)

/**
 * Что именно сделало добавление.
 *
 * `alreadyHad` — не ошибка: слово, которое у человека уже было, осталось там, где лежало.
 */
@Serializable
data class ImportResultDto(
    val categoryId: Int = 0,
    val name: String = "",
    val added: Int = 0,
    val alreadyHad: Int = 0
)

@Serializable
data class ImportResultResponse(
    val status: String,
    val data: ImportResultDto? = null,
    val message: String? = null
)
