package com.wordwaverise.wordwaveriseapp.presentation.saved

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity

data class SavedWordsState(
    val words: List<SavedWordEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isOffline: Boolean = false,
    val showCategorySheet: Boolean = false,
    /** Запись, для которой открыт выбор папок. Строка, а не написание: значений может быть два. */
    val entryToFile: SavedWordEntity? = null,
    /** Отмеченные папки, пока лист открыт. */
    val chosenFolders: Set<Long> = emptySet(),
    val newCategoryName: String = "",
    /** Ссылка, которую надо отдать системному листу «Поделиться»; одноразовая. */
    val pendingShareUrl: String? = null,
    /** Ссылка на чужую папку, которую человек вставил. */
    val importLink: String = "",
    val importing: Boolean = false,
    /** Итог добавления словами — включая то, что осталось лежать на своих местах. */
    val importMessage: String? = null
) {
    /**
     * ⚠️ «Все» показывает **свой** словарь, без слов преподавателя.
     *
     * Их там может быть в разы больше, чем своих, и тогда собственный список перестаёт быть
     * своим. Папка класса открывается своим чипом — там они и нужны.
     */
    val filteredWords: List<SavedWordEntity>
        get() = if (selectedCategoryId == null) words.filter { !it.readOnly }
                else words.filter { selectedCategoryId in it.categoryIds }

    /**
     * Папки, куда слово действительно можно положить, — только свои.
     *
     * ⚠️ Папки класса здесь нет. Сервер и раньше отказывал в записи в чужую папку, а лист её
     * всё равно предлагал: человек выбирал папку и получал молчаливый отказ. Предлагать то,
     * что заведомо не сработает, хуже, чем не предлагать.
     */
    val ownCategories: List<CategoryEntity>
        get() = categories.filter { !it.readOnly }
}
