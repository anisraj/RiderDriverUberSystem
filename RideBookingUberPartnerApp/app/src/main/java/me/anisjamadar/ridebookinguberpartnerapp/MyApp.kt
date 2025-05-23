package me.anisjamadar.ridebookinguberpartnerapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.ola.maps.sdk.core.config.PlatformConfig
import dagger.hilt.android.HiltAndroidApp
import me.anisjamadar.locationsharingapplication.BuildConfig

const val APP_TAG = "LocationSharingApp"

@HiltAndroidApp
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            "location",
            "Location",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        PlatformConfig.Builder()
            .apiKey(BuildConfig.API_KEY)
            .baseUrl("https://api.olamaps.io")
            .maxRetryAttempts(3)
            .build()
    }
}