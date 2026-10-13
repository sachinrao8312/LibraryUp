package ai.sach.libraryup.ui.library

import ai.sach.libraryup.BuildConfig
import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Free-tier banner. Only for lists and the dashboard; never on payment or receipt screens. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    AndroidView(modifier.fillMaxWidth(), factory = { context ->
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerSize(context, widthDp))
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    })
}

/** Loads and shows a rewarded ad; [onReward] runs once the owner has watched it. */
fun showRewardedAd(activity: Activity, onReward: () -> Unit, onFailed: () -> Unit) {
    RewardedAd.load(activity, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedAd) = ad.show(activity) { onReward() }
        override fun onAdFailedToLoad(error: LoadAdError) = onFailed()
    })
}
