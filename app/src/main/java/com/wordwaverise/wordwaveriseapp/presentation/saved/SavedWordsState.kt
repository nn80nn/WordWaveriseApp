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
    /** Серверный id папки-группы, внутрь которой создаётся следующая папка. */
    val newCategoryParentServerId: Int? = null,
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

    /**
     * Папки деревом: группа, сразу за ней — то, что в ней лежит.
     *
     * Строится по **видимому** списку: папка класса, выданная без своего модуля, обязана стоять
     * корнем, а не пропасть между уровнями. Родителя, которого нет в списке, сервер уже обнулил,
     * но проверка нужна и здесь — синхронизация может застать список наполовину обновлённым.
     */
    val folderRows: List<FolderRow>
        get() {
            val visible = categories.mapNotNull { it.serverId }.toSet()
            val childrenOf = categories
                .filter { it.parentServerId != null && it.parentServerId in visible }
                .groupBy { it.parentServerId!! }
            return categories
                .filter { it.parentServerId == null || it.parentServerId !in visible }
                .flatMap { root ->
                    listOf(FolderRow(root, nested = false)) +
                        childrenOf[root.serverId].orEmpty().map { FolderRow(it, nested = true) }
                }
        }

    /** То же дерево, но только из своих папок: в чужую слово положить нельзя. */
    val ownFolderRows: List<FolderRow> get() = folderRows.filter { !it.folder.readOnly }

    /**
     * Свои корневые папки — единственные, что могут быть группой.
     *
     * Вложенность ровно одна, и сервер это проверяет. Предложить папку, которая уже лежит в
     * группе, значит вести человека к отказу, который он не сможет объяснить.
     */
    val groupCandidates: List<CategoryEntity>
        get() = categories.filter { !it.readOnly && it.parentServerId == null && it.serverId != null }

    /** Сколько папок лежит внутри этой. Ноль — обычная папка. */
    fun childCount(folder: CategoryEntity): Int =
        folder.serverId?.let { id -> categories.count { it.parentServerId == id } } ?: 0
}

/** Папка и её место в дереве. Уровней всего два, поэтому флага достаточно. */
data class FolderRow(val folder: CategoryEntity, val nested: Boolean)
