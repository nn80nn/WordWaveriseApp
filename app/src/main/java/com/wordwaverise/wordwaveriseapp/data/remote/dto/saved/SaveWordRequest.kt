package com.wordwaverise.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SaveWordRequest(
    val word: String,
    val translation: String? = null,
    val definition: String? = null,
    /**
     * Pins the word to one sense of its article.
     *
     * The wording is then read from the corpus server-side and the [translation]/[definition]
     * sent alongside are ignored — the client picks a sense, not a phrasing, and only the
     * server can promise that the same pick means the same thing here and in the browser.
     * ⚠️ Сохранение под **другим** значением заводит вторую запись, а не переставляет
     * первую: отметить два определения — значит попросить два слова. Единственное исключение —
     * слово, сохранённое вообще без значения: там привязка вписывается в ту же строку.
     */
    val senseId: String? = null,

    /** Разложить сразу. Отсутствие списка — «никуда», и это не то же, что пустой список. */
    val categoryIds: List<Int>? = null
)
