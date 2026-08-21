package me.asrielyankare.gachaultils

import android.app.Application
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

        // Initialize NewBlackbox integration
        // Currently uses stub mode until NewBlackbox AAR is available
        blackBoxIntegration = NewBlackboxIntegration(this)
        blackBoxIntegration.initialize()
        blackBoxIntegration.registerImplementations()
    }
}
