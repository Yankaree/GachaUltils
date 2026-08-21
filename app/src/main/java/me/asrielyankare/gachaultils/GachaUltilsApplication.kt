package me.asrielyankare.gachaultils

import android.app.Application
import me.asrielyankare.gachaultils.core.InstanceStorage
import me.asrielyankare.gachaultils.blackbox.StubBlackBoxIntegration

/**
 * Application class that initializes core services at startup.
 */
class GachaUltilsApplication : Application() {

    lateinit var blackBoxIntegration: StubBlackBoxIntegration
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize persistent instance storage
        InstanceStorage.init(filesDir)

        // Initialize BlackBox integration (stub mode until NewBlackbox is available)
        blackBoxIntegration = StubBlackBoxIntegration(this)
        blackBoxIntegration.initialize()
        blackBoxIntegration.registerImplementations()
    }
}
