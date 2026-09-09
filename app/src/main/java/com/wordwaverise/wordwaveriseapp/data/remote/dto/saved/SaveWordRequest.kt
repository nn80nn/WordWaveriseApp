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
    val categoryIds: List<Int>? = null,

    /**
     * Откуда слово взято: предложение книги и место слова в нём.
     *
     * ⚠️ Отправляется, когда значение назвать нечем — статьи нет либо подсказка не выбрала его
     * уверенно. Сервер тогда **не подставляет первое** значение статьи, а достраивает статью в
     * фоне и выбирает то значение, в котором слово стояло в тексте. Без контекста в словарь
     * ложится карточка про другой смысл.
     */
    val context: SaveContext? = null
)

/** Предложение, в котором слово встретилось. */
@Serializable
data class SaveContext(
    val sentence: String,
    val tokenIndex: Int? = null,
    /** Часть речи в этом предложении — так, как её назвала подсказка. */
    val pos: String? = null
)
