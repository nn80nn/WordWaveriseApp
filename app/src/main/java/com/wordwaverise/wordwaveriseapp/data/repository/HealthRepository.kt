package com.wordwaverise.wordwaveriseapp.data.repository

import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.HealthResponse
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun checkHealth(): Resource<HealthResponse> {
        return try {
            val response = apiService.getHealth()
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
