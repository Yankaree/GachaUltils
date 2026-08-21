package me.asrielyankare.gachaultils

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import me.asrielyankare.gachaultils.core.InstanceStorage

/**
 * Application class that initializes core services at startup.
 * NewBlackbox is NOT initialized here to avoid crash on app start.
 * It is initialized lazily when first needed (e.g. import APK).
 */
class GachaUltilsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize persistent instance storage
        InstanceStorage.init(filesDir)

        // Create notification channels early
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val blackboxChannel = NotificationChannel(
                "blackbox_service",
                "BlackBox Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "NewBlackbox background service"
                setShowBadge(false)
            }

            val generalChannel = NotificationChannel(
                "gacha_general",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            manager.createNotificationChannel(blackboxChannel)
            manager.createNotificationChannel(generalChannel)
        }
    }
}
