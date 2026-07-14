package com.gymgym.app.ads

import android.app.Application

/** Free flavor: real AdMob-backed ads. */
fun provideAdManager(app: Application): AdManager = AdMobManager()
