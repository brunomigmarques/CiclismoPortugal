package com.ciclismo.portugal.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.ciclismo.portugal.data.local.premium.PremiumManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Play Billing for Premium subscriptions.
 *
 * Product IDs (create these in Google Play Console):
 * - premium_monthly: Monthly subscription at 1.99
 * - premium_annual: Annual subscription at 14.99
 *
 * Setup in Google Play Console:
 * 1. Go to Monetize > Subscriptions
 * 2. Create "premium_monthly" subscription with base plan
 * 3. Create "premium_annual" subscription with base plan
 * 4. Publish the app to internal testing track to test purchases
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumManager: PremiumManager
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"

        // Product IDs - must match Google Play Console
        const val PRODUCT_MONTHLY = "premium_monthly"
        const val PRODUCT_ANNUAL = "premium_annual"

        // Reconnection settings
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val RECONNECT_DELAY_MS = 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var billingClient: BillingClient? = null
    private var reconnectAttempts = 0

    // Product details cache
    private val _monthlyProduct = MutableStateFlow<ProductDetails?>(null)
    val monthlyProduct: StateFlow<ProductDetails?> = _monthlyProduct.asStateFlow()

    private val _annualProduct = MutableStateFlow<ProductDetails?>(null)
    val annualProduct: StateFlow<ProductDetails?> = _annualProduct.asStateFlow()

    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Purchase state
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    /**
     * Initialize billing client and connect.
     * Call this when the app starts.
     */
    fun initialize() {
        Log.d(TAG, "Initializing BillingManager")

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    /**
     * Start connection to Google Play Billing.
     */
    private fun startConnection() {
        if (billingClient?.isReady == true) {
            Log.d(TAG, "BillingClient already connected")
            return
        }

        Log.d(TAG, "Starting BillingClient connection...")
        billingClient?.startConnection(this)
    }

    // ========== BillingClientStateListener ==========

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d(TAG, "Billing setup finished: ${billingResult.responseCode}")

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _isConnected.value = true
            reconnectAttempts = 0

            // Query products and existing purchases
            scope.launch {
                queryProducts()
                queryExistingPurchases()
            }
        } else {
            _isConnected.value = false
            Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected")
        _isConnected.value = false

        // Try to reconnect
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            scope.launch {
                kotlinx.coroutines.delay(RECONNECT_DELAY_MS * reconnectAttempts)
                startConnection()
            }
        }
    }

    // ========== PurchasesUpdatedListener ==========

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        Log.d(TAG, "Purchases updated: ${billingResult.responseCode}")

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
                _purchaseState.value = PurchaseState.Cancelled
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned, restoring...")
                scope.launch { queryExistingPurchases() }
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _purchaseState.value = PurchaseState.Error(
                    "Erro na compra: ${billingResult.debugMessage}"
                )
            }
        }
    }

    // ========== Product Queries ==========

    /**
     * Query available subscription products.
     */
    private suspend fun queryProducts() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Querying products...")

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { product ->
                    when (product.productId) {
                        PRODUCT_MONTHLY -> _monthlyProduct.value = product
                        PRODUCT_ANNUAL -> _annualProduct.value = product
                    }
                }
                Log.d(TAG, "Found ${productDetailsList.size} products")
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Query existing purchases (for restore).
     */
    private suspend fun queryExistingPurchases() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Querying existing purchases...")

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Found ${purchases.size} existing purchases")

                if (purchases.isEmpty()) {
                    // No active subscriptions
                    scope.launch {
                        premiumManager.setPremiumStatus(false, 0L)
                    }
                } else {
                    purchases.forEach { purchase ->
                        handlePurchase(purchase)
                    }
                }
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    // ========== Purchase Flow ==========

    /**
     * Launch purchase flow for monthly subscription.
     */
    fun purchaseMonthly(activity: Activity) {
        val product = _monthlyProduct.value
        if (product == null) {
            Log.e(TAG, "Monthly product not available")
            _purchaseState.value = PurchaseState.Error("Produto nao disponivel. Tenta novamente.")
            return
        }
        launchPurchaseFlow(activity, product)
    }

    /**
     * Launch purchase flow for annual subscription.
     */
    fun purchaseAnnual(activity: Activity) {
        val product = _annualProduct.value
        if (product == null) {
            Log.e(TAG, "Annual product not available")
            _purchaseState.value = PurchaseState.Error("Produto nao disponivel. Tenta novamente.")
            return
        }
        launchPurchaseFlow(activity, product)
    }

    /**
     * Launch the billing flow for a product.
     */
    private fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        Log.d(TAG, "Launching purchase flow for ${productDetails.productId}")
        _purchaseState.value = PurchaseState.Loading

        // Get the offer token (required for subscriptions)
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "No offer token found for ${productDetails.productId}")
            _purchaseState.value = PurchaseState.Error("Erro ao iniciar compra")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)

        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${billingResult?.debugMessage}")
            _purchaseState.value = PurchaseState.Error("Erro ao abrir pagamento")
        }
    }

    // ========== Purchase Handling ==========

    /**
     * Handle a successful purchase.
     */
    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "Handling purchase: ${purchase.products}, state: ${purchase.purchaseState}")

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                // Verify and acknowledge the purchase
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                } else {
                    // Already acknowledged, activate premium
                    activatePremium(purchase)
                }
            }
            Purchase.PurchaseState.PENDING -> {
                Log.d(TAG, "Purchase pending")
                _purchaseState.value = PurchaseState.Pending
            }
            else -> {
                Log.d(TAG, "Purchase state: ${purchase.purchaseState}")
            }
        }
    }

    /**
     * Acknowledge a purchase (required within 3 days).
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        Log.d(TAG, "Acknowledging purchase: ${purchase.orderId}")

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
                activatePremium(purchase)
            } else {
                Log.e(TAG, "Failed to acknowledge: ${billingResult.debugMessage}")
                _purchaseState.value = PurchaseState.Error("Erro ao confirmar compra")
            }
        }
    }

    /**
     * Activate premium status after successful purchase.
     */
    private fun activatePremium(purchase: Purchase) {
        Log.d(TAG, "Activating premium for products: ${purchase.products}")

        scope.launch {
            // Calculate expiry based on product type
            val isAnnual = purchase.products.contains(PRODUCT_ANNUAL)
            val durationMs = if (isAnnual) {
                TimeUnit.DAYS.toMillis(365)
            } else {
                TimeUnit.DAYS.toMillis(30)
            }
            val expiryDate = System.currentTimeMillis() + durationMs

            // Update premium status
            premiumManager.setPremiumStatus(
                isPremium = true,
                expiryDate = expiryDate
            )

            _purchaseState.value = PurchaseState.Success(
                productId = purchase.products.firstOrNull() ?: "",
                isAnnual = isAnnual
            )

            Log.d(TAG, "Premium activated until: $expiryDate")
        }
    }

    // ========== Restore Purchases ==========

    /**
     * Restore previous purchases.
     */
    fun restorePurchases() {
        Log.d(TAG, "Restoring purchases...")
        _purchaseState.value = PurchaseState.Loading

        if (billingClient?.isReady != true) {
            _purchaseState.value = PurchaseState.Error("Servico de pagamento nao disponivel")
            startConnection()
            return
        }

        scope.launch {
            queryExistingPurchases()
        }
    }

    // ========== Pricing Info ==========

    /**
     * Get formatted monthly price.
     */
    fun getMonthlyPrice(): String? {
        return _monthlyProduct.value?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
    }

    /**
     * Get formatted annual price.
     */
    fun getAnnualPrice(): String? {
        return _annualProduct.value?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
    }

    // ========== Cleanup ==========

    /**
     * End billing connection.
     * Call this when the app is destroyed.
     */
    fun endConnection() {
        Log.d(TAG, "Ending billing connection")
        billingClient?.endConnection()
        billingClient = null
        _isConnected.value = false
    }

    /**
     * Reset purchase state (after showing result to user).
     */
    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
}

/**
 * Purchase state for UI.
 */
sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Loading : PurchaseState()
    data object Pending : PurchaseState()
    data object Cancelled : PurchaseState()
    data class Success(val productId: String, val isAnnual: Boolean) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
