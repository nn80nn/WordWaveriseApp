package com.wordwaverise.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

/** Весь набор папок одной записи. Пустой список — «без папки». */
@Serializable
data class SetWordFoldersRequest(
    val categoryIds: List<Int> = emptyList()
)
