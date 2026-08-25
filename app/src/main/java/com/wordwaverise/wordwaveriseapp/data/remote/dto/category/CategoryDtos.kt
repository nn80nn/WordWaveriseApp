package com.wordwaverise.wordwaveriseapp.data.remote.dto.category

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: Int,
    val name: String,
    val color: String? = null,
    /** Свои слова плюс детские, если это папка-группа, — то же число, что даст фильтр. */
    val wordCount: Int = 0,

    /**
     * Папка-группа, в которой лежит эта папка, или null.
     *
     * Ровно один уровень: папка с родителем сама родителем не бывает, поэтому дерево всегда
     * двухэтажное и рисуется по одному этому полю.
     *
     * ⚠️ Null значит ещё и «родителя вам не видно»: учитель может выдать один урок без модуля,
     * и сервер обрезает ссылку, а не называет папку, которую читатель всё равно не откроет.
     */
    val parentId: Int? = null,

    /** Заполнено, когда папку одолжила группа: слова в ней принадлежат преподавателю. */
    val groupId: Int? = null,
    val groupName: String? = null,
    /** Папка группы: переименовать, удалить и класть в неё слова нельзя. */
    val readOnly: Boolean = false
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val color: String? = null,
    /** Создаёт папку сразу внутри папки-группы. */
    val parentId: Int? = null
)

/**
 * Вложить папку в группу или вынуть обратно (`parentId = null`).
 *
 * Отдельно от переименования: на одном запросе «в корень» и «не трогать родителя» были бы
 * одними и теми же байтами.
 */
@Serializable
data class SetParentRequest(val parentId: Int? = null)

@Serializable
data class RenameCategoryRequest(val name: String)

@Serializable
data class SetWordCategoryRequest(val categoryId: Int? = null)

@Serializable
data class CategoriesResponse(
    val status: String,
    val data: List<CategoryDto>? = null,
    val message: String? = null
)

@Serializable
data class CategoryResponse(
    val status: String,
    val data: CategoryDto? = null,
    val message: String? = null
)

@Serializable
data class SimpleStringResponse(
    val status: String,
    val data: String? = null,
    val message: String? = null
)
