package com.gymgym.app.ads

import android.app.Activity
import android.app.Application
import android.content.Context

/** Paid flavor: no ads, no SDK, no network. Every hook is a no-op. */
fun provideAdManager(app: Application): AdManager = object : AdManager {
    override fun warmUp(context: Context) {}
    override fun onWorkoutOpen(activity: Activity, start: () -> Unit) = start()
}
