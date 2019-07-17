package com.jxsun.devfinder

import android.app.Application
import com.jxsun.devfinder.di.appModule
import org.koin.core.context.startKoin
import timber.log.Timber

class DevFinderApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        startKoin { modules(appModule) }
    }
}