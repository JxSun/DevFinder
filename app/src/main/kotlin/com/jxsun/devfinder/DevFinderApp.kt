package com.jxsun.devfinder

import android.app.Application
import timber.log.Timber

class DevFinderApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
    }
}