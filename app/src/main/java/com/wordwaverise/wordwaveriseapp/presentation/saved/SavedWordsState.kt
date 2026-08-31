package com.wordwaverise.wordwaveriseapp.presentation.saved

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity

/** Порядок слов в списке. Тот же набор, что на сайте: список один и тот же. */
enum class WordSort { NEWEST, OLDEST, A_Z, Z_A }

/** Порядок папок. `CUSTOM` — как их завели, то есть порядок сервера. */
enum class FolderSort { CUSTOM, A_Z, Z_A, COUNT }

data class SavedWordsState(
    val words: List<SavedWordEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    /** Поиск по своим словам: написание, перевод. */
    val searchQuery: String = "",
    val sortBy: WordSort = WordSort.NEWEST,
    /** Поиск по папкам — живёт только пока открыт лист папок. */
    val folderQuery: String = "",
    val folderSort: FolderSort = FolderSort.CUSTOM,
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
        get() {
            val scope = when (selectedCategoryId) {
                null -> null
                UNCATEGORIZED -> null
                else -> scopeOf(selectedCategoryId)
            }
            val inFolder = when {
                selectedCategoryId == null -> words.filter { !it.readOnly }
                selectedCategoryId == UNCATEGORIZED ->
                    words.filter { !it.readOnly && it.categoryIds.isEmpty() }
                else -> words.filter { word -> word.categoryIds.any { it in scope!! } }
            }
            val needle = searchQuery.trim().lowercase()
            val found = if (needle.isEmpty()) inFolder else inFolder.filter { word ->
                word.word.lowercase().contains(needle) ||
                    word.translation?.lowercase()?.contains(needle) == true
            }
            return when (sortBy) {
                WordSort.NEWEST -> found.sortedByDescending { it.savedAt }
                WordSort.OLDEST -> found.sortedBy { it.savedAt }
                WordSort.A_Z -> found.sortedBy { it.word.lowercase() }
                WordSort.Z_A -> found.sortedByDescending { it.word.lowercase() }
            }
        }

    /**
     * Папка и всё, что в ней лежит.
     *
     * ⚠️ Названная папка-группа достаёт и вложенные — ровно как на сервере. Иначе у модуля
     * стоял бы счётчик из детских слов и пустой список под ним: два правдивых числа об одной
     * папке, из которых одно всегда врёт.
     */
    fun scopeOf(categoryId: Long): Set<Long> {
        val serverId = categories.firstOrNull { it.id == categoryId }?.serverId
            ?: return setOf(categoryId)
        return buildSet {
            add(categoryId)
            categories.filter { it.parentServerId == serverId }.forEach { add(it.id) }
        }
    }

    /**
     * Сколько своих слов в каждой папке; у папки-группы — вместе с вложенными.
     *
     * Число на чипе — единственное, что отличает полную папку от пустой до того, как в неё
     * зашли, и «0» рядом с кнопкой «практиковать» — число, с которым нечего делать.
     */
    val wordCounts: Map<Long, Int>
        get() {
            val parentOf = categories
                .filter { it.parentServerId != null }
                .mapNotNull { child ->
                    categories.firstOrNull { it.serverId == child.parentServerId }
                        ?.let { child.id to it.id }
                }
                .toMap()
            val counts = mutableMapOf<Long, Int>()
            for (word in words) {
                // Слово в двух уроках одного модуля — одно слово модуля, а не два.
                val scope = buildSet {
                    for (id in word.categoryIds) {
                        add(id)
                        parentOf[id]?.let { add(it) }
                    }
                }
                for (id in scope) counts[id] = (counts[id] ?: 0) + 1
            }
            return counts
        }

    /** Свои слова, не лежащие ни в одной папке. Ноль прячет чип: нажимать не на что. */
    val looseCount: Int
        get() = words.count { !it.readOnly && it.categoryIds.isEmpty() }

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
        get() = treeFor(folderQuery).flatMap { node ->
            listOf(FolderRow(node.folder, nested = false)) +
                node.children.map { FolderRow(it, nested = true) }
        }

    /**
     * Корневые папки с их содержимым — то, из чего рисуется ряд фильтра.
     *
     * ⚠️ Без поиска: строка поиска живёт в листе папок, и отфильтрованный ею ряд наверху
     * прятал бы половину папок на экране, где этой строки не видно.
     */
    val rootFolders: List<FolderNode> get() = treeFor("")

    /**
     * Дерево папок: группа и то, что в ней лежит, в выбранном порядке.
     *
     * ⚠️ Строится по **видимому** списку: папка класса, выданная без своего модуля, обязана
     * стоять корнем, а не пропасть между уровнями. Родителя, которого нет в списке, сервер уже
     * обнулил, но проверка нужна и здесь — синхронизация может застать список наполовину
     * обновлённым.
     *
     * ⚠️ Группа остаётся в списке, даже если совпало только имя вложенной папки: найденный
     * «Урок 5» иначе повис бы в воздухе, а родитель — единственное, что отличает его от
     * «Урока 5» из соседнего модуля.
     */
    private fun treeFor(query: String): List<FolderNode> {
        val needle = query.trim().lowercase()
        val counts = wordCounts
        val matches = { folder: CategoryEntity ->
            needle.isEmpty() || folder.name.lowercase().contains(needle)
        }
        val order = when (folderSort) {
            FolderSort.CUSTOM -> compareBy<CategoryEntity> { 0 }
            FolderSort.A_Z -> compareBy { it.name.lowercase() }
            FolderSort.Z_A -> compareByDescending { it.name.lowercase() }
            FolderSort.COUNT -> compareByDescending { counts[it.id] ?: 0 }
        }

        val visible = categories.mapNotNull { it.serverId }.toSet()
        val childrenOf = categories
            .filter { it.parentServerId != null && it.parentServerId in visible }
            .groupBy { it.parentServerId!! }

        return categories
            .filter { it.parentServerId == null || it.parentServerId !in visible }
            .map { root ->
                val kids = childrenOf[root.serverId].orEmpty()
                val self = matches(root)
                FolderNode(root, (if (self) kids else kids.filter(matches)).sortedWith(order))
            }
            .filter { matches(it.folder) || it.children.isNotEmpty() }
            .sortedWith(compareBy(order) { it.folder })
    }

    /**
     * Группа, содержимое которой раскрыто вторым рядом: выбранная папка или родитель выбранной.
     *
     * Раскрыта и тогда, когда выбран урок внутри неё: зайдя в урок, человек иначе терял и
     * соседние уроки, и дорогу обратно к модулю.
     */
    val openGroup: CategoryEntity?
        get() {
            val id = selectedCategoryId ?: return null
            if (id == UNCATEGORIZED) return null
            val selected = categories.firstOrNull { it.id == id } ?: return null
            selected.parentServerId?.let { parent ->
                return categories.firstOrNull { it.serverId == parent }
            }
            return selected.takeIf { childCount(it) > 0 }
        }

    /** Вложенные папки раскрытой группы, в том же порядке, что и всё остальное. */
    val openChildren: List<CategoryEntity>
        get() {
            val group = openGroup ?: return emptyList()
            return rootFolders.firstOrNull { it.folder.id == group.id }?.children.orEmpty()
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

    companion object {
        /**
         * «Без папки» — не первая папка, а её отсутствие: слово лежит в скольких угодно папок.
         *
         * То же число, что у сервера (`UNCATEGORIZED_CATEGORY_ID`), у веба (`UNCATEGORIZED`) и
         * у выбора папки в заданиях (`FolderOption.UNCATEGORIZED`).
         */
        const val UNCATEGORIZED = -1L
    }
}

/** Папка-группа вместе с тем, что в ней лежит. Уровней два, поэтому вложенности нет. */
data class FolderNode(val folder: CategoryEntity, val children: List<CategoryEntity>)

/** Папка и её место в дереве. Уровней всего два, поэтому флага достаточно. */
data class FolderRow(val folder: CategoryEntity, val nested: Boolean)
