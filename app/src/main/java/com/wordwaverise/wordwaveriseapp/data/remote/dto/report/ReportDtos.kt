package com.wordwaverise.wordwaveriseapp.data.remote.dto.report

import kotlinx.serialization.Serializable

/**
 * Жалоба на сгенерированный текст: статью, значение или вопрос упражнения.
 *
 * Называет то, что было на экране, а не сам текст: статья переписывается из корпуса, и копия
 * текста здесь протухла бы ровно в тот момент, когда её начнут разбирать.
 */
@Serializable
data class ContentReportRequest(
    /** `article`, `exercise` или `other` — список закрыт, сервер отвергает незнакомое. */
    val kind: String,
    val word: String,
    val senseId: String? = null,
    /** `wrong` | `offensive` | `nonsense` | `other`. */
    val reason: String,
    val comment: String? = null
)

@Serializable
data class ContentReportResponse(
    val status: String,
    val message: String? = null
)
