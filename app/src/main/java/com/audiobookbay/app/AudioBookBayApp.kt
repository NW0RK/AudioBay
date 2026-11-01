package com.audiobookbay.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AudioBookBayApp : Application() {
    @javax.inject.Inject lateinit var crashReports: com.audiobookbay.app.diagnostics.CrashReports

    override fun onCreate() {
        super.onCreate()
        crashReports.installHandler()
        crashReports.recoverInterruptedSession()
        crashReports.collectStartupLogs()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
