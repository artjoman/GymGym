package com.gymgym.app.ads

import android.app.Activity
import android.content.Context

/**
 * Shows interstitial ads at "open a workout" boundaries only — never mid-set.
 *
 * Two implementations exist, selected by product flavor: a real AdMob one in
 * the `free` source set and a no-op in `paid`. The concrete instance is created
 * by the per-flavor [provideAdManager] factory, so the paid build never links
 * the ad SDK.
 */
interface AdManager {

    /** Initialize consent + the ad SDK and preload the first ad. Safe to no-op. */
    fun warmUp(context: Context)

    /**
     * Called when the user opens an exercise/plan/auto. Shows an ad if one is
     * due (see the cooldown / first-workout rules), then runs [start]; if no ad
     * is shown, runs [start] immediately. [start] is the action that actually
     * begins the workout.
     */
    fun onWorkoutOpen(activity: Activity, start: () -> Unit)
}
