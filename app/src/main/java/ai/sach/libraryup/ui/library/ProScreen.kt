package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.billing.ProManager
import ai.sach.libraryup.data.repository.LibraryState
import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Plan status, what Pro adds, and the ways to get it: yearly, lifetime, or a 24-hour pass for an ad. */
@Composable
fun ProScreen(state: LibraryState, vm: LibraryViewModel, onBack: () -> Unit) {
    val pro = vm.pro
    val activity = LocalContext.current as Activity
    val trialLeft = pro.trialDaysLeft(state.profile)
    val status = when {
        pro.purchased -> stringResource(R.string.pro_status_active)
        trialLeft > 0 -> stringResource(R.string.pro_status_trial, trialLeft)
        vm.isPro -> stringResource(R.string.pro_status_pass)
        else -> stringResource(R.string.pro_status_free, pro.seatLimit)
    }

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.title_pro), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(status, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            listOf(
                stringResource(R.string.pro_benefit_seats), stringResource(R.string.pro_benefit_staff),
                stringResource(R.string.pro_benefit_cards), stringResource(R.string.pro_benefit_enquiries),
                stringResource(R.string.pro_benefit_brand), stringResource(R.string.pro_benefit_report),
                stringResource(R.string.pro_benefit_no_ads)
            ).forEach { benefit ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusColors.paid)
                    Spacer(Modifier.width(8.dp))
                    Text(benefit)
                }
            }
            Spacer(Modifier.height(24.dp))
            if (!pro.purchased) {
                if (pro.products.isEmpty()) Text(stringResource(R.string.pro_prices_loading), style = MaterialTheme.typography.bodySmall)
                pro.products.forEach { product ->
                    val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                        ?: product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                    val label = if (product.productId == ProManager.PRO_LIFETIME) R.string.pro_buy_lifetime else R.string.pro_buy_yearly
                    Button(onClick = { pro.buy(activity, product) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(stringResource(label, price ?: ""))
                    }
                }
                OutlinedButton(
                    onClick = { showRewardedAd(activity, onReward = { pro.grantPass(); vm.message(R.string.pro_pass_granted) }, onFailed = { vm.message(R.string.pro_ad_failed) }) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text(stringResource(R.string.pro_watch_ad)) }
                TextButton(onClick = pro::refresh) { Text(stringResource(R.string.pro_restore)) }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.pro_never_locked), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
