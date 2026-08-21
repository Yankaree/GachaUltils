package me.asrielyankare.gachaultils

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import me.asrielyankare.gachaultils.core.InstanceStorage
import me.asrielyankare.gachaultils.blackbox.NewBlackboxIntegration

/**
 * Application class that initializes core services at startup.
 */
class GachaUltilsApplication : Application() {

    lateinit var blackBoxIntegration: NewBlackboxIntegration
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize persistent instance storage
        InstanceStorage.init(filesDir)

        // Create notification channels BEFORE NewBlackbox init
        // NewBlackbox DaemonService needs a notification channel for foreground service
        createNotificationChannels()

        // Initialize NewBlackbox integration
        try {
            blackBoxIntegration = NewBlackboxIntegration(this)
            blackBoxIntegration.initialize()
            blackBoxIntegration.registerImplementations()
        } catch (e: Exception) {
            // NewBlackbox init failed — app can still work with limited functionality
            android.util.Log.e("GachaUltils", "NewBlackbox init failed: ${e.message}", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            // BlackBox DaemonService channel
            val blackboxChannel = NotificationChannel(
                "blackbox_service",
                "BlackBox Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "NewBlackbox background service"
                setShowBadge(false)
            }

            // General notifications channel
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
