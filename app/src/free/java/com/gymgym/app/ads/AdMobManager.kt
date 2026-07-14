package com.gymgym.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.gymgym.app.BuildConfig

/**
 * AdMob interstitials, shown only when a workout is opened and only if due:
 * the first workout of a session (lastAdAtMs starts at 0) and then no more
 * often than [BuildConfig.AD_INTERVAL_MS]. Never called mid-set.
 */
class AdMobManager : AdManager {

    private var initialized = false
    private var consentObtained = false
    private var interstitial: InterstitialAd? = null
    private var loading = false
    private var lastAdAtMs = 0L // 0 => the first workout always shows an ad

    override fun warmUp(context: Context) {
        if (!BuildConfig.ADS_ENABLED) return
        val consent = UserMessagingPlatform.getConsentInformation(context)
        consent.requestConsentInfoUpdate(
            context.findActivity() ?: return initAfterConsent(context, consent),
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    context.findActivity() ?: return@requestConsentInfoUpdate,
                ) { initAfterConsent(context, consent) }
            },
            { error -> Log.w(TAG, "Consent update failed: ${error.message}"); initAfterConsent(context, consent) },
        )
    }

    private fun initAfterConsent(context: Context, consent: ConsentInformation) {
        consentObtained = consent.canRequestAds()
        if (!consentObtained) return
        if (!initialized) {
            MobileAds.initialize(context.applicationContext) {}
            initialized = true
        }
        preload(context.applicationContext)
    }

    private fun preload(context: Context) {
        if (loading || interstitial != null || !consentObtained) return
        loading = true
        InterstitialAd.load(
            context,
            BuildConfig.AD_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loading = false
                    Log.w(TAG, "Interstitial load failed: ${error.message}")
                }
            },
        )
    }

    override fun onWorkoutOpen(activity: Activity, start: () -> Unit) {
        val due = System.currentTimeMillis() - lastAdAtMs >= BuildConfig.AD_INTERVAL_MS
        val ad = interstitial
        if (!BuildConfig.ADS_ENABLED || !consentObtained || !due || ad == null) {
            if (consentObtained) preload(activity.applicationContext)
            start()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                lastAdAtMs = System.currentTimeMillis()
                interstitial = null
                preload(activity.applicationContext)
                start()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                preload(activity.applicationContext)
                start()
            }
        }
        ad.show(activity)
    }

    private fun Context.findActivity(): Activity? = this as? Activity

    private companion object {
        const val TAG = "AdMobManager"
    }
}
