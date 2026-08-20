package com.wordwaverise.wordwaveriseapp.data.remote.dto.group

import kotlinx.serialization.Serializable

/**
 * Класс, как его видит конкретный человек.
 *
 * `inviteUrl` и `joinCode` приходят только владельцу — они и есть разрешение вступить, и ученик,
 * который прочитал бы их со своего экрана, мог бы раздать класс кому угодно. На телефоне
 * администрирования нет вовсе, так что здесь они просто не используются.
 */
@Serializable
data class GroupDto(
    val id: Int,
    val name: String,
    val createdAt: String = "",
    val isOwner: Boolean = false,
    val memberCount: Int = 0,
    val folderCount: Int = 0,
    val assignmentCount: Int = 0,
    val teacherName: String? = null
)

@Serializable
data class MyGroupsDto(
    val owned: List<GroupDto> = emptyList(),
    val joined: List<GroupDto> = emptyList()
)

@Serializable
data class JoinByCodeRequest(val code: String)

@Serializable
data class AssignmentDto(
    val id: Int,
    val groupId: Int,
    val groupName: String = "",
    val title: String,
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val exerciseTarget: Int? = null,
    val reviewTarget: Int? = null,
    val kinds: List<String> = emptyList(),
    val dueAt: String? = null,
    val createdAt: String = ""
)

/** Задание вместе с тем, сколько по нему уже сделано. */
@Serializable
data class StudentAssignmentDto(
    val assignment: AssignmentDto,
    val exercisesDone: Int = 0,
    val reviewsDone: Int = 0,
    val percent: Int = 0,
    val completed: Boolean = false,
    val overdue: Boolean = false,
    val lastAttemptAt: String? = null
)

/**
 * Один ответ.
 *
 * `clientAttemptId` создаётся в момент ответа, а не отправки: тогда повторная отправка — это
 * «дубликат» на сервере, а не «плюс один» к прогрессу ученика.
 */
@Serializable
data class AttemptDto(
    val clientAttemptId: String,
    val activity: String,
    val kind: String? = null,
    val word: String,
    val cardId: Int? = null,
    val verdict: String,
    val answeredAt: String
)

@Serializable
data class ReportAttemptsRequest(
    val groupId: Int,
    val assignmentId: Int? = null,
    val categoryId: Int? = null,
    val attempts: List<AttemptDto> = emptyList()
)

@Serializable
data class ReportAttemptsResult(
    val accepted: Int = 0,
    val duplicates: Int = 0,
    val rejected: Int = 0
)

// ── Конверты ─────────────────────────────────────────────────────────────

@Serializable
data class MyGroupsResponse(
    val status: String = "",
    val data: MyGroupsDto? = null,
    val message: String? = null
)

@Serializable
data class GroupResponse(
    val status: String = "",
    val data: GroupDto? = null,
    val message: String? = null
)

@Serializable
data class AssignmentsResponse(
    val status: String = "",
    val data: List<StudentAssignmentDto>? = null,
    val message: String? = null
)

@Serializable
data class ReportAttemptsResponse(
    val status: String = "",
    val data: ReportAttemptsResult? = null,
    val message: String? = null
)
