package com.wordwaverise.wordwaveriseapp.data.remote.dto.category

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: Int,
    val name: String,
    val color: String? = null,
    val wordCount: Int = 0
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val color: String? = null
)

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
