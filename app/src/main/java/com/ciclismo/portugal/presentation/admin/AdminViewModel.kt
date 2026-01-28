package com.ciclismo.portugal.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.repository.ProvaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: ProvaRepository
) : ViewModel() {

    val allProvas: StateFlow<List<Prova>> = repository.getAllProvasAdmin()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun hideProva(provaId: Long) {
        viewModelScope.launch {
            repository.hideProva(provaId)
        }
    }

    fun showProva(provaId: Long) {
        viewModelScope.launch {
            repository.showProva(provaId)
        }
    }

    fun deleteProva(provaId: Long) {
        viewModelScope.launch {
            repository.deleteProva(provaId)
        }
    }

    fun clearAllAndResync() {
        viewModelScope.launch {
            repository.clearAllProvas()
            repository.syncProvas()
        }
    }
}
