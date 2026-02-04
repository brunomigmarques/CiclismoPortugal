package com.ciclismo.portugal.presentation.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.repository.CyclistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PriceReductionUiState {
    object Loading : PriceReductionUiState()
    data class Updating(
        val currentIndex: Int,
        val totalCyclists: Int,
        val currentName: String,
        val successCount: Int,
        val errorCount: Int
    ) : PriceReductionUiState()
    data class Success(
        val totalCyclists: Int,
        val averagePrice: Double,
        val categoryStats: Map<CyclistCategory, CategoryStats>
    ) : PriceReductionUiState()
    data class Error(val message: String) : PriceReductionUiState()
}

data class CategoryStats(
    val count: Int,
    val averagePrice: Double
)

@HiltViewModel
class PriceReductionViewModel @Inject constructor(
    private val cyclistRepository: CyclistRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PriceReductionVM"
    }

    private val _uiState = MutableStateFlow<PriceReductionUiState>(PriceReductionUiState.Loading)
    val uiState: StateFlow<PriceReductionUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                _uiState.value = PriceReductionUiState.Loading

                val cyclists = cyclistRepository.getValidatedCyclists().first()

                if (cyclists.isEmpty()) {
                    _uiState.value = PriceReductionUiState.Success(
                        totalCyclists = 0,
                        averagePrice = 0.0,
                        categoryStats = emptyMap()
                    )
                    return@launch
                }

                val totalCyclists = cyclists.size
                val averagePrice = cyclists.map { it.price }.average()

                val categoryStats = cyclists.groupBy { it.category }.mapValues { (_, cyclistList) ->
                    CategoryStats(
                        count = cyclistList.size,
                        averagePrice = cyclistList.map { it.price }.average()
                    )
                }

                _uiState.value = PriceReductionUiState.Success(
                    totalCyclists = totalCyclists,
                    averagePrice = averagePrice,
                    categoryStats = categoryStats
                )

                Log.d(TAG, "Loaded stats: $totalCyclists cyclists, avg price: $averagePrice")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stats: ${e.message}", e)
                _uiState.value = PriceReductionUiState.Error("Erro ao carregar estatísticas: ${e.message}")
            }
        }
    }

    fun applyPriceReduction(reductionPercent: Int, category: CyclistCategory?) {
        viewModelScope.launch {
            try {
                _uiState.value = PriceReductionUiState.Loading

                val multiplier = 1.0 - (reductionPercent / 100.0)

                val cyclists = cyclistRepository.getValidatedCyclists().first()
                val targetCyclists = if (category != null) {
                    cyclists.filter { it.category == category }
                } else {
                    cyclists
                }

                Log.d(TAG, "========== APPLYING PRICE REDUCTION ==========")
                Log.d(TAG, "Reduction: $reductionPercent%")
                Log.d(TAG, "Multiplier: $multiplier")
                Log.d(TAG, "Target cyclists: ${targetCyclists.size}")
                Log.d(TAG, "Category filter: ${category?.name ?: "ALL"}")

                var successCount = 0
                var errorCount = 0
                val totalToUpdate = targetCyclists.size

                targetCyclists.forEachIndexed { index, cyclist ->
                    // Update progress state
                    _uiState.value = PriceReductionUiState.Updating(
                        currentIndex = index + 1,
                        totalCyclists = totalToUpdate,
                        currentName = cyclist.fullName,
                        successCount = successCount,
                        errorCount = errorCount
                    )

                    try {
                        val oldPrice = cyclist.price
                        val newPrice = (oldPrice * multiplier).coerceAtLeast(1.0) // Minimum price of 1.0M
                        val roundedNewPrice = (newPrice * 10).toLong() / 10.0 // Round to 1 decimal place

                        Log.d(TAG, "Updating ${cyclist.fullName}: ${oldPrice}M -> ${roundedNewPrice}M")

                        // Create updated cyclist with new price and update directly
                        val updatedCyclist = cyclist.copy(price = roundedNewPrice)
                        val result = cyclistRepository.updateCyclist(updatedCyclist)

                        if (result.isSuccess) {
                            successCount++
                            Log.d(TAG, "SUCCESS: ${cyclist.fullName} updated to ${roundedNewPrice}M")
                        } else {
                            errorCount++
                            Log.e(TAG, "FAILED: ${cyclist.fullName} - ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "Error reducing price for ${cyclist.fullName}: ${e.message}")
                    }
                }

                Log.d(TAG, "========== PRICE REDUCTION COMPLETE ==========")
                Log.d(TAG, "Success: $successCount, Errors: $errorCount")

                val categoryText = category?.let { getCategoryDisplayName(it) } ?: "todos os ciclistas"

                if (errorCount == 0) {
                    _message.value = "Redução de $reductionPercent% aplicada com sucesso a $successCount $categoryText!"
                } else {
                    _message.value = "Redução aplicada: $successCount sucesso, $errorCount erros"
                }

                // Reload stats to show updated prices
                loadStats()
            } catch (e: Exception) {
                Log.e(TAG, "Error applying reduction: ${e.message}", e)
                _message.value = "Erro ao aplicar redução: ${e.message}"
                loadStats() // Reload to restore UI state
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun getCategoryDisplayName(category: CyclistCategory): String {
        return when (category) {
            CyclistCategory.CLIMBER -> "escaladores"
            CyclistCategory.HILLS -> "punchers"
            CyclistCategory.TT -> "contrarrelogistas"
            CyclistCategory.SPRINT -> "sprinters"
            CyclistCategory.GC -> "ciclistas GC"
            CyclistCategory.ONEDAY -> "especialistas clássicas"
        }
    }
}
