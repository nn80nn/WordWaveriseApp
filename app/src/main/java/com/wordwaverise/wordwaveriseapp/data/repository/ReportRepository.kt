package com.wordwaverise.wordwaveriseapp.data.repository

import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.report.ContentReportRequest
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun report(
        kind: String,
        word: String,
        senseId: String?,
        reason: String,
        comment: String?
    ): Resource<Unit> {
        return try {
            val token = tokenDataStore.token.firstOrNull()
            if (token.isNullOrEmpty()) return Resource.Error("Войдите, чтобы отправить жалобу")
            apiService.reportContent(
                "Bearer $token",
                ContentReportRequest(
                    kind = kind,
                    word = word,
                    senseId = senseId,
                    reason = reason,
                    comment = comment?.trim()?.ifBlank { null }
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
