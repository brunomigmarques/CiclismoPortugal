package com.ciclismo.portugal.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.NewsArticle
import com.ciclismo.portugal.domain.usecase.GetNewsUseCase
import com.ciclismo.portugal.domain.usecase.SyncNewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val getNewsUseCase: GetNewsUseCase,
    private val syncNewsUseCase: SyncNewsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadNews()
        syncNews()
    }

    private fun loadNews() {
        viewModelScope.launch {
            getNewsUseCase()
                .catch { e ->
                    _uiState.value = NewsUiState.Error(e.message ?: "Erro ao carregar notícias")
                }
                .collect { news ->
                    _uiState.value = if (news.isEmpty()) {
                        NewsUiState.Empty
                    } else {
                        NewsUiState.Success(news)
                    }
                }
        }
    }

    fun syncNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncNewsUseCase()
                // Pequeno delay para garantir que o utilizador vê o refresh
                delay(500)
                // loadNews will automatically update via Flow
            } catch (e: Exception) {
                // Error is logged in repository
                _uiState.value = NewsUiState.Error(e.message ?: "Erro ao sincronizar notícias")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

sealed class NewsUiState {
    object Loading : NewsUiState()
    object Empty : NewsUiState()
    data class Success(val news: List<NewsArticle>) : NewsUiState()
    data class Error(val message: String) : NewsUiState()
}
