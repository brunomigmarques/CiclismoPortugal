package com.ciclismo.portugal.presentation.premium

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.billing.BillingManager
import com.ciclismo.portugal.data.billing.PurchaseState
import com.ciclismo.portugal.data.local.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Premium subscription screen.
 *
 * Handles subscription purchase flow via Google Play Billing.
 */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumManager: PremiumManager,
    private val billingManager: BillingManager
) : ViewModel() {

    companion object {
        private const val TAG = "PremiumViewModel"
    }

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    // Store activity reference for billing flow
    private var currentActivity: Activity? = null

    init {
        loadPremiumStatus()
        observeBillingState()
        observeProducts()
    }

    private fun loadPremiumStatus() {
        viewModelScope.launch {
            val isPremium = premiumManager.isPremium()
            val trialDays = premiumManager.getRemainingTrialDays()

            _uiState.value = _uiState.value.copy(
                isPremium = isPremium,
                trialDaysRemaining = trialDays
            )
        }
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            billingManager.purchaseState.collect { state ->
                when (state) {
                    PurchaseState.Idle -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    PurchaseState.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    PurchaseState.Pending -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Pagamento pendente. Sera processado em breve."
                        )
                    }
                    PurchaseState.Cancelled -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = null // No message for user cancellation
                        )
                        billingManager.resetPurchaseState()
                    }
                    is PurchaseState.Success -> {
                        val planName = if (state.isAnnual) "Anual" else "Mensal"
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPremium = true,
                            message = "Premium $planName ativado com sucesso!"
                        )
                        billingManager.resetPurchaseState()
                    }
                    is PurchaseState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = state.message
                        )
                        billingManager.resetPurchaseState()
                    }
                }
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            combine(
                billingManager.monthlyProduct,
                billingManager.annualProduct,
                billingManager.isConnected
            ) { monthly, annual, connected ->
                Triple(monthly, annual, connected)
            }.collect { (monthly, annual, connected) ->
                _uiState.value = _uiState.value.copy(
                    monthlyPrice = billingManager.getMonthlyPrice(),
                    annualPrice = billingManager.getAnnualPrice(),
                    isProductsLoaded = monthly != null && annual != null,
                    isBillingConnected = connected
                )
            }
        }
    }

    /**
     * Set the current activity for billing flow.
     * Call this from the screen's LaunchedEffect.
     */
    fun setActivity(activity: Activity) {
        currentActivity = activity
    }

    /**
     * Clear activity reference.
     * Call this from onDispose.
     */
    fun clearActivity() {
        currentActivity = null
    }

    /**
     * Purchase monthly subscription.
     */
    fun purchaseMonthly() {
        val activity = currentActivity
        if (activity == null) {
            Log.e(TAG, "Activity not set, cannot launch billing flow")
            _uiState.value = _uiState.value.copy(
                message = "Erro interno. Tenta novamente."
            )
            return
        }

        if (!billingManager.isConnected.value) {
            _uiState.value = _uiState.value.copy(
                message = "Servico de pagamento nao disponivel. Tenta novamente."
            )
            return
        }

        Log.d(TAG, "Initiating monthly purchase")
        billingManager.purchaseMonthly(activity)
    }

    /**
     * Purchase annual subscription.
     */
    fun purchaseAnnual() {
        val activity = currentActivity
        if (activity == null) {
            Log.e(TAG, "Activity not set, cannot launch billing flow")
            _uiState.value = _uiState.value.copy(
                message = "Erro interno. Tenta novamente."
            )
            return
        }

        if (!billingManager.isConnected.value) {
            _uiState.value = _uiState.value.copy(
                message = "Servico de pagamento nao disponivel. Tenta novamente."
            )
            return
        }

        Log.d(TAG, "Initiating annual purchase")
        billingManager.purchaseAnnual(activity)
    }

    /**
     * Restore previous purchases.
     */
    fun restorePurchases() {
        Log.d(TAG, "Restoring purchases")
        billingManager.restorePurchases()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class PremiumUiState(
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val trialDaysRemaining: Int = 7,
    val message: String? = null,
    val monthlyPrice: String? = null,
    val annualPrice: String? = null,
    val isProductsLoaded: Boolean = false,
    val isBillingConnected: Boolean = false
)
