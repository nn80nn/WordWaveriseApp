package com.wordwaverise.wordwaveriseapp.presentation.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.repository.HealthRepository
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _state = mutableStateOf(MainState())
    val state: State<MainState> = _state

    fun checkConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )

            when (val result = healthRepository.checkHealth()) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Соединение успешно! Статус: ${result.data?.status}",
                        error = null
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message,
                        successMessage = null
                    )
                }
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }
}
