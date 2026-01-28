package com.ciclismo.portugal.presentation.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // Test Ad Unit IDs from Google
        const val BANNER_AD_UNIT_ID = "ca-app-pub-4498446920337333/8835929871"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4498446920337333/7055944347"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-4498446920337333/5996151723"
    }

    private var interstitialAd: InterstitialAd? = null

    init {
        MobileAds.initialize(context) {}
    }

    fun createAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    fun loadInterstitialAd(onAdLoaded: () -> Unit = {}, onAdFailedToLoad: (LoadAdError) -> Unit = {}) {
        val adRequest = createAdRequest()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    onAdFailedToLoad(error)
                }
            }
        )
    }

    fun showInterstitialAd(activity: android.app.Activity, onAdDismissed: () -> Unit = {}) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed()
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                }
            }
            ad.show(activity)
        } ?: run {
            onAdDismissed()
        }
    }
}
