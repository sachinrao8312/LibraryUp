package ai.sach.libraryup.billing

import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.domain.ProRules
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Pro status: Play Billing purchases (yearly subscription or lifetime), the trial, and the
 * 24-hour pass from a rewarded ad. The free-tier seat limit and trial length come from Remote
 * Config. The last known purchase is cached so Pro keeps working offline.
 */
class ProManager(context: Context) : PurchasesUpdatedListener {
    private val prefs = context.getSharedPreferences("pro", Context.MODE_PRIVATE)
    private val scope = MainScope()
    private val billing = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    var purchased by mutableStateOf(prefs.getBoolean(KEY_PURCHASED, false))
        private set
    var passUntil by mutableLongStateOf(prefs.getLong(KEY_PASS_UNTIL, 0))
        private set
    var products by mutableStateOf<List<ProductDetails>>(emptyList())
        private set
    var seatLimit by mutableIntStateOf(ProRules.FREE_SEAT_LIMIT)
        private set
    var trialDays by mutableIntStateOf(ProRules.TRIAL_DAYS)
        private set

    fun trialDaysLeft(profile: LibraryProfile?): Int =
        profile?.let { ProRules.trialDaysLeft(it.trialStartedOn, LocalDate.now(), trialDays) } ?: trialDays

    fun isPro(profile: LibraryProfile?): Boolean =
        ProRules.isPro(purchased, trialDaysLeft(profile), passUntil, System.currentTimeMillis())

    fun start() {
        runCatching {
            val config = FirebaseRemoteConfig.getInstance()
            config.setDefaultsAsync(mapOf(KEY_SEAT_LIMIT to ProRules.FREE_SEAT_LIMIT.toLong(), KEY_TRIAL_DAYS to ProRules.TRIAL_DAYS.toLong()))
            config.fetchAndActivate().addOnCompleteListener {
                seatLimit = config.getLong(KEY_SEAT_LIMIT).toInt()
                trialDays = config.getLong(KEY_TRIAL_DAYS).toInt()
            }
        }.onFailure { Log.w(TAG, "Remote Config unavailable", it) }
        if (billing.isReady) return refresh()
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) refresh()
            }

            override fun onBillingServiceDisconnected() = Unit // start() runs again on the next app start
        })
    }

    /** Re-reads purchases from Play ("Restore purchases") and loads prices. */
    fun refresh() {
        if (!billing.isReady) return start()
        scope.launch {
            val owned = listOf(ProductType.SUBS, ProductType.INAPP).flatMap { type ->
                billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()).purchasesList
            }
            handle(owned, fromPlayQuery = true)
            products = listOf(PRO_YEARLY to ProductType.SUBS, PRO_LIFETIME to ProductType.INAPP).mapNotNull { (id, type) ->
                val product = QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()
                billing.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
                    .productDetailsList?.firstOrNull()
            }
        }
    }

    fun buy(activity: Activity, product: ProductDetails) {
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
        product.subscriptionOfferDetails?.firstOrNull()?.let { params.setOfferToken(it.offerToken) }
        billing.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params.build())).build())
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK && purchases != null) scope.launch { handle(purchases, fromPlayQuery = false) }
    }

    /** A rewarded ad was watched: Pro for the next 24 hours. */
    fun grantPass() {
        passUntil = System.currentTimeMillis() + PASS_MILLIS
        prefs.edit().putLong(KEY_PASS_UNTIL, passUntil).apply()
    }

    private suspend fun handle(purchases: List<Purchase>, fromPlayQuery: Boolean) {
        val ours = purchases.filter { p -> p.purchaseState == Purchase.PurchaseState.PURCHASED && p.products.any { it in PRODUCT_IDS } }
        ours.filter { !it.isAcknowledged }.forEach {
            billing.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
        }
        // A full query is the truth (a lapsed subscription disappears); an update only ever adds.
        if (fromPlayQuery || ours.isNotEmpty()) {
            purchased = ours.isNotEmpty()
            prefs.edit().putBoolean(KEY_PURCHASED, purchased).apply()
        }
    }

    companion object {
        const val PRO_YEARLY = "pro_yearly"
        const val PRO_LIFETIME = "pro_lifetime"
        private val PRODUCT_IDS = setOf(PRO_YEARLY, PRO_LIFETIME)
        private const val PASS_MILLIS = 24 * 60 * 60 * 1000L
        private const val KEY_PURCHASED = "purchased"
        private const val KEY_PASS_UNTIL = "pass_until"
        private const val KEY_SEAT_LIMIT = "free_seat_limit"
        private const val KEY_TRIAL_DAYS = "trial_days"
        private const val TAG = "ProManager"
    }
}
